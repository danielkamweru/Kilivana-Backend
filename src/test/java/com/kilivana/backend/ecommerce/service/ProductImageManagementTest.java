package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.service.*;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.entity.*;
import com.kilivana.backend.ecommerce.repository.*;
import com.kilivana.backend.security.ProductVisibility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductImageManagementTest {
    final ProductRepository products=mock(ProductRepository.class);
    final ProductImageRepository images=mock(ProductImageRepository.class);
    ImageStorage storage=mock(ImageStorage.class);
    ProductService service;
    final Product product=Product.builder().id(1L).sellerId(4L).status(ProductStatus.UNLISTED).stockQty(12).reservedQty(0).build();
    final ProductImage old=ProductImage.builder().id(2L).productId(1L).publicId("old").url("old-url").build();
    @BeforeEach void setup() {
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(products.save(any())).thenAnswer(i -> i.getArgument(0));
        when(images.save(any())).thenAnswer(i -> i.getArgument(0));
        when(images.findById(2L)).thenReturn(Optional.of(old));
        when(images.findByProductId(1L)).thenReturn(List.of(old));
        when(images.findByProductIdOrderBySortOrderAsc(1L)).thenReturn(List.of(old));
        rebuild();
    }
    void rebuild() { service=new ProductService(products,images,mock(CategoryRepository.class),storage,mock(UserService.class),mock(ProductVisibility.class)); }
    MockMultipartFile png() { return new MockMultipartFile("image","photo.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,0}); }
    void uploadResult() throws Exception {
        when(storage.uploadImage(any(),anyString())).thenReturn(Map.of("id","new"));
        when(storage.getPublicId(any())).thenReturn("new");
        when(storage.getSecureUrl(any())).thenReturn("new-url");
    }
    @AfterEach void clear() { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); }
    @Test void addAndReplaceReturnUpdatedImageUrl() throws Exception {
        uploadResult();
        assertEquals("new-url",service.uploadProductImage(1L,png(),1,false).getUrl());
        var response=service.updateProductImage(1L,2L,png(),0,true);
        assertEquals("new-url",response.getUrl());
        verify(storage).deleteImage("old");
        verify(storage,never()).replaceImage(any(),any(),any());
        assertEquals("new-url",service.getProductById(1L).getImages().get(0).getUrl());
    }
    @Test void removeUsesStoredIdentifierAndRejectsForeignProductImage() throws Exception {
        service.deleteProductImage(1L,2L);
        verify(storage).deleteImage("old");
        verify(images).delete(old);
        old.setProductId(9L); clearInvocations(storage,images);
        assertThrows(BadRequestException.class,()->service.deleteProductImage(1L,2L));
        assertThrows(BadRequestException.class,()->service.updateProductImage(1L,2L,png(),0,true));
        verifyNoInteractions(storage);
        verify(images,never()).delete(any());
    }
    @Test void rejectInvalidTypeContentSizeAndCountBeforeStorage() {
        for (var file:List.of(new MockMultipartFile("image","x.txt","text/plain",new byte[]{1}),
                new MockMultipartFile("image","fake.png","image/png",new byte[]{1}),
                new MockMultipartFile("image","empty.png","image/png",new byte[0]),
                new MockMultipartFile("image","big.png","image/png",new byte[10*1024*1024+1])))
            assertThrows(BadRequestException.class,()->service.uploadProductImage(1L,file,0,false));
        when(images.findByProductId(1L)).thenReturn(Collections.nCopies(10,old));
        assertThrows(BadRequestException.class,()->service.uploadProductImage(1L,png(),0,false));
        verifyNoInteractions(storage);
    }
    @Test void failedReplacementKeepsOriginalImage() throws Exception {
        when(storage.uploadImage(any(),anyString())).thenThrow(new IOException("simulated"));
        assertThrows(IOException.class,()->service.updateProductImage(1L,2L,png(),0,true));
        assertEquals("old",old.getPublicId()); assertEquals("old-url",old.getUrl());
        verify(storage,never()).deleteImage(any());
        verify(images,never()).save(old);
    }
    @Test void multipartEditAppendsAndPreservesOldImage() throws Exception {
        uploadResult();
        service.updateProductWithImages(1L,ProductRequest.builder().stockQty(12).name("Updated").build(),List.of(png()));
        verify(images,never()).deleteAll(anyIterable());
        verify(storage,never()).deleteImage("old");
        assertEquals("old-url",old.getUrl());
    }
    @Test void batchValidatesAllFilesBeforeUploadingAny() {
        assertThrows(BadRequestException.class,()->service.updateProductWithImages(1L,
                ProductRequest.builder().stockQty(12).build(),List.of(png(),new MockMultipartFile("image",new byte[]{1}))));
        verifyNoInteractions(storage);
    }
    @Test void reorderRejectsForeignAndDuplicateIds() {
        assertThrows(BadRequestException.class,()->service.reorderImages(1L,List.of(9L)));
        assertThrows(BadRequestException.class,()->service.reorderImages(1L,List.of(2L,2L)));
        verify(images,never()).save(any());
    }
    @Test void localReplacementRollbackRemovesNewFileAndPreservesOld(@TempDir Path directory) throws Exception {
        storage=new LocalImageStorage(directory.toString(),"http://localhost"); rebuild();
        var stored=storage.uploadImage(png(),"kilivana/products/1");
        String original=storage.getPublicId(stored); old.setPublicId(original);
        TransactionSynchronizationManager.initSynchronization();
        service.updateProductImage(1L,2L,png(),0,false);
        String replacement=old.getPublicId();
        assertTrue(Files.exists(directory.resolve(original)));
        assertTrue(Files.exists(directory.resolve(replacement)));
        for(var callback:TransactionSynchronizationManager.getSynchronizations()) callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertTrue(Files.exists(directory.resolve(original)));
        assertFalse(Files.exists(directory.resolve(replacement)));
    }
    @Test void localRemovalWaitsForCommit(@TempDir Path directory) throws Exception {
        storage=new LocalImageStorage(directory.toString(),"http://localhost"); rebuild();
        old.setPublicId(storage.getPublicId(storage.uploadImage(png(),"kilivana/products/1")));
        TransactionSynchronizationManager.initSynchronization();
        service.deleteProductImage(1L,2L);
        assertTrue(Files.exists(directory.resolve(old.getPublicId())));
        for(var callback:TransactionSynchronizationManager.getSynchronizations()) callback.afterCommit();
        assertFalse(Files.exists(directory.resolve(old.getPublicId())));
    }
}

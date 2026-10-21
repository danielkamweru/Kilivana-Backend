package com.kilivana.backend.farm.controller;

import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminFarmVisibilityTest {
    @Test void rosterPreservesNameOwnerAndMetadataWithoutCreatingRecords() {
        FarmRepository farms=mock(FarmRepository.class);
        var page=PageRequest.of(0,1);
        var farm=Farm.builder().id(7L).farmerId(4L).name("Green Valley").status(FarmStatus.ACTIVE).build();
        when(farms.searchFarms("Green",null,FarmStatus.ACTIVE,4L,page)).thenReturn(new PageImpl<>(List.of(farm),page,2));
        var result=new FarmController(farms,mock(FarmImageRepository.class)).listFarms(4L,null,FarmStatus.ACTIVE,"Green",page).getBody().getData();
        var items=(List<?>)result.get("items");
        var item=(com.kilivana.backend.farm.dto.FarmResponse)items.get(0);
        assertEquals("Green Valley",item.getName()); assertEquals(4L,item.getFarmerId());
        assertEquals(FarmStatus.ACTIVE,item.getStatus()); assertEquals(2L,result.get("totalItems"));
        assertEquals(2,result.get("totalPages"));
        verify(farms,never()).save(any());
    }
    @Test void missingAssociationReturnsEmptyPageWithoutInventingAFarm() {
        FarmRepository farms=mock(FarmRepository.class); var page=PageRequest.of(3,10);
        when(farms.searchFarms(null,null,null,4L,page)).thenReturn(Page.empty(page));
        var result=new FarmController(farms,mock(FarmImageRepository.class)).listFarms(4L,null,null,null,page).getBody().getData();
        assertEquals(List.of(),result.get("items")); assertEquals(0L,result.get("totalItems"));
        verify(farms,never()).save(any());
    }
    @Test void nativeFilterBindsEnumNameAndLeavesOmittedStatusNull() {
        FarmRepository farms=mock(FarmRepository.class,CALLS_REAL_METHODS); var page=PageRequest.of(0,10);
        farms.searchFarms("Green","Kiambu",FarmStatus.ACTIVE,4L,page);
        verify(farms).searchFarmsByStatusName("Green","Kiambu","ACTIVE",4L,page);
        farms.searchFarms(null,null,null,null,page);
        verify(farms).searchFarmsByStatusName(null,null,null,null,page);
    }
}

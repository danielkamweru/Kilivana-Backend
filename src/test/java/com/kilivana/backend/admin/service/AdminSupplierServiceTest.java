package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.AdminSupplierResponse;
import com.kilivana.backend.admin.entity.SupplierProfile;
import com.kilivana.backend.admin.entity.SupplierWarehouse;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.SupplierProfileRepository;
import com.kilivana.backend.admin.repository.SupplierWarehouseRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.service.UserReferenceCodeGenerator;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSupplierServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SupplierProfileRepository supplierProfileRepository;
    @Mock private SupplierWarehouseRepository supplierWarehouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserReferenceCodeGenerator referenceCodeGenerator;
    @InjectMocks private AdminSupplierService service;

    @Test
    void getSupplierIncludesBusinessDetailsAndWarehouseInformation() {
        User supplier = supplier();
        SupplierProfile profile = SupplierProfile.builder()
                .id(12L)
                .userId(30L)
                .businessName("Green Valley Farms Ltd")
                .businessDetails("Wholesale vegetable supplier")
                .verificationInfo("Business License BL-2026-045")
                .location("Nakuru")
                .build();
        SupplierWarehouse warehouse = SupplierWarehouse.builder()
                .id(51L)
                .userId(30L)
                .name("Nakuru Central Warehouse")
                .address("Plot 45, Nakuru-Eldoret Road")
                .county("Nakuru")
                .latitude(-0.3031)
                .longitude(36.0800)
                .contactName("Mary Wanjiku")
                .contactPhone("+254722000003")
                .active(true)
                .defaultLocation(true)
                .build();

        when(userRepository.findById(30L)).thenReturn(Optional.of(supplier));
        when(supplierProfileRepository.findByUserId(30L)).thenReturn(Optional.of(profile));
        when(supplierWarehouseRepository.findByUserIdOrderById(30L)).thenReturn(List.of(warehouse));
        when(productRepository.findBySellerIdAndSellerType(30L, SellerType.SUPPLIER)).thenReturn(List.of());

        AdminSupplierResponse response = service.getSupplier(30L);

        assertEquals("Mary Wanjiku", response.getContactPerson());
        assertEquals("brian@example.test", response.getEmail());
        assertEquals("Wholesale vegetable supplier", response.getBusinessDetails());
        assertEquals("Business License BL-2026-045", response.getVerificationInfo());
        assertEquals(1, response.getWarehouses().size());
        assertEquals("Nakuru Central Warehouse", response.getWarehouses().get(0).getName());
        assertEquals("Nakuru", response.getWarehouses().get(0).getCounty());
        assertEquals("+254722000003", response.getWarehouses().get(0).getContactPhone());
        verify(supplierWarehouseRepository).findByUserIdOrderById(30L);
    }

    @Test
    void listSuppliersLoadsWarehousesInOneBatch() {
        User supplier = supplier();
        SupplierWarehouse warehouse = SupplierWarehouse.builder()
                .id(51L)
                .userId(30L)
                .name("Nakuru Central Warehouse")
                .address("Plot 45")
                .county("Nakuru")
                .latitude(-0.3031)
                .longitude(36.0800)
                .build();

        when(userRepository.findByRole(UserRole.SUPPLIER)).thenReturn(List.of(supplier));
        when(supplierProfileRepository.findAll()).thenReturn(List.of());
        when(supplierWarehouseRepository.findByUserIdInOrderById(List.of(30L)))
                .thenReturn(List.of(warehouse));

        List<AdminSupplierResponse> responses = service.listSuppliers();

        assertEquals(1, responses.get(0).getWarehouses().size());
        assertEquals("Nakuru Central Warehouse", responses.get(0).getWarehouses().get(0).getName());
        verify(supplierWarehouseRepository).findByUserIdInOrderById(List.of(30L));
    }

    private static User supplier() {
        return User.builder()
                .id(30L)
                .name("Mary Wanjiku")
                .email("brian@example.test")
                .phone("+254722000003")
                .username("greenvalley")
                .region("Nakuru")
                .referenceCode("S-001")
                .passwordHash("not-returned")
                .role(UserRole.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.VERIFIED)
                .emailVerified(true)
                .build();
    }
}

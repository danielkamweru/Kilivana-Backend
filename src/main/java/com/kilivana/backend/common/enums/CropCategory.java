package com.kilivana.backend.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Broad grouping of a crop by the kind of produce it yields.
 */
@Schema(description = "Broad grouping of a crop by the kind of produce it yields.")
public enum CropCategory {
    CEREAL,
    LEGUME,
    VEGETABLE,
    FRUIT,
    CASH_CROP,
    ROOT_TUBER,
    HERBS_SPICES,
    OTHER
}

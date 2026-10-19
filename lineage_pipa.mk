#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

# Inherit some common lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_tablet_wifionly.mk)

# Inherit from pipa device
$(call inherit-product, device/xiaomi/pipa/device.mk)

PRODUCT_NAME := lineage_pipa
PRODUCT_DEVICE := pipa
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := 23043RP34G

PRODUCT_CHARACTERISTICS := tablet
TARGET_SUPPORTS_QUICK_TAP := false

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PIPA_FINGERPRINT := Xiaomi/pipa_pad_cn/pipa:14/UKQ1.240624.001/OS2.0.20.0.UMZCNXM:user/release-keys

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=$(PIPA_FINGERPRINT)

# Evolution X replaces the build fingerprint with a Pixel one. Keep the real one
# here for the apps that are given it back, like the setup wizard.
PRODUCT_SYSTEM_PROPERTIES += \
    ro.product.fingerprint=$(PIPA_FINGERPRINT)

# Evolution X
EVO_BUILD_TYPE := Unofficial
WITH_GMS := true

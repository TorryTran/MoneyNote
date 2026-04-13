package com.moneynote.app.ui.common

import com.moneynote.app.R

object CategoryVisuals {
    private val iconMap = mapOf(
        "Ăn uống" to R.drawable.ic_cat_food,
        "Chi tiêu hàng ngày" to R.drawable.ic_cat_daily,
        "Quần áo" to R.drawable.ic_cat_clothes,
        "Mỹ phẩm" to R.drawable.ic_cat_cosmetic,
        "Phí giao lưu" to R.drawable.ic_cat_social,
        "Y tế" to R.drawable.ic_cat_medical,
        "Giáo dục" to R.drawable.ic_cat_education,
        "Tiền điện" to R.drawable.ic_cat_electric,
        "Đi lại" to R.drawable.ic_cat_transport,
        "Phí liên lạc" to R.drawable.ic_cat_communication,
        "Tiền nhà" to R.drawable.ic_cat_house,
        "Tiền lương" to R.drawable.ic_cat_salary,
        "Tiền phụ" to R.drawable.ic_cat_extra,
        "Tiền thưởng" to R.drawable.ic_cat_bonus,
        "Thu nhập khác" to R.drawable.ic_cat_other_income,
        "Đầu tư" to R.drawable.ic_cat_invest,
        "Chuyển khoản nội bộ" to R.drawable.ic_wallet_tab,
        "Chỉnh sửa" to R.drawable.ic_cat_edit,
        "Sửa danh mục" to R.drawable.ic_cat_edit
    )

    fun iconResFor(category: String): Int {
        return iconMap[category] ?: R.drawable.ic_cat_edit
    }
}

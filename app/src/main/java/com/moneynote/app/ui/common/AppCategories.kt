package com.moneynote.app.ui.common

import com.moneynote.app.R

object AppCategories {
    fun expenseDefault() = mutableListOf(
        CategoryItem("Ăn uống", R.drawable.ic_cat_food, "#FF9F43"),
        CategoryItem("Chi tiêu hàng ngày", R.drawable.ic_cat_daily, "#2ECC71"),
        CategoryItem("Quần áo", R.drawable.ic_cat_clothes, "#385DFF"),
        CategoryItem("Mỹ phẩm", R.drawable.ic_cat_cosmetic, "#FF66C3"),
        CategoryItem("Phí giao lưu", R.drawable.ic_cat_social, "#F8D347"),
        CategoryItem("Y tế", R.drawable.ic_cat_medical, "#5DE0C7"),
        CategoryItem("Giáo dục", R.drawable.ic_cat_education, "#FF6C7B"),
        CategoryItem("Tiền điện", R.drawable.ic_cat_electric, "#48CAFF"),
        CategoryItem("Đi lại", R.drawable.ic_cat_transport, "#D18A4C"),
        CategoryItem("Phí liên lạc", R.drawable.ic_cat_communication, "#7B8BA5"),
        CategoryItem("Tiền nhà", R.drawable.ic_cat_house, "#F584C4"),
        CategoryItem("Sửa danh mục", R.drawable.ic_cat_edit, "#FFFFFF", isEditor = true)
    )

    fun incomeDefault() = mutableListOf(
        CategoryItem("Tiền lương", R.drawable.ic_cat_salary, "#2ECC71"),
        CategoryItem("Tiền phụ", R.drawable.ic_cat_extra, "#FF9F43"),
        CategoryItem("Tiền thưởng", R.drawable.ic_cat_bonus, "#FF6C7B"),
        CategoryItem("Thu nhập khác", R.drawable.ic_cat_other_income, "#48CAFF"),
        CategoryItem("Đầu tư", R.drawable.ic_cat_invest, "#5DE0C7"),
        CategoryItem("Sửa danh mục", R.drawable.ic_cat_edit, "#FFFFFF", isEditor = true)
    )
}

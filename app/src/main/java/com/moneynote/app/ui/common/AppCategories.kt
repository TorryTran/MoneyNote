package com.moneynote.app.ui.common

import com.moneynote.app.R

object AppCategories {
    fun expenseDefault() = mutableListOf(
        CategoryItem("Ăn uống", R.drawable.ic_cat_food, "#FF9F43"),
        CategoryItem("Mua sắm", R.drawable.ic_cat_clothes, "#385DFF"),
        CategoryItem("Đi chợ", R.drawable.ic_cat_daily, "#2ECC71"),
        CategoryItem("Nhà cửa", R.drawable.ic_cat_house, "#F584C4"),
        CategoryItem("Đi lại", R.drawable.ic_cat_transport, "#D18A4C"),
        CategoryItem("Y tế", R.drawable.ic_cat_medical, "#5DE0C7"),
        CategoryItem("Điện nước", R.drawable.ic_cat_electric, "#48CAFF"),
        CategoryItem("Con cái", R.drawable.ic_cat_social, "#F8D347"),
        CategoryItem("Trả nợ", R.drawable.ic_cat_daily, "#FF6C7B"),
        CategoryItem("Giáo dục", R.drawable.ic_cat_education, "#B57CFF"),
        CategoryItem("Điện thoại", R.drawable.ic_cat_communication, "#7B8BA5"),
        CategoryItem("Sửa danh mục", R.drawable.ic_cat_edit, "#FFFFFF", isEditor = true)
    )

    fun incomeDefault() = mutableListOf(
        CategoryItem("Tiền lương", R.drawable.ic_cat_salary, "#2ECC71"),
        CategoryItem("Tiền thưởng", R.drawable.ic_cat_bonus, "#FF6C7B"),
        CategoryItem("Trợ cấp", R.drawable.ic_cat_extra, "#FF9F43"),
        CategoryItem("Làm thêm", R.drawable.ic_cat_daily, "#48CAFF"),
        CategoryItem("Vay mượn", R.drawable.ic_cat_social, "#F8D347"),
        CategoryItem("Đầu tư", R.drawable.ic_cat_invest, "#5DE0C7"),
        CategoryItem("Tiền khác", R.drawable.ic_cat_other_income, "#B57CFF"),
        CategoryItem("Sửa danh mục", R.drawable.ic_cat_edit, "#FFFFFF", isEditor = true)
    )
}

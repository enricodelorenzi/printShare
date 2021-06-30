package com.androidexam.printshare.utilities;

public class MaterialListItem {

    private String material;
    private boolean checked;

    public MaterialListItem(String material) {
        this.material = material;
        this.checked = false;
    }

    public String getMaterial() {
        return material;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}

package com.zkar.pis.remotecontrol;
public class Plugin {
	private String Name;
	private String Action;
	private String Text;
	private String ImageNormal;
	private String imageFocus;
	private String DisplayOrder;
	private String PackageName;
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getAction() {
		return Action;
	}
	public void setAction(String action) {
		Action = action;
	}
	public String getText() {
		return Text;
	}
	public void setText(String text) {
		Text = text;
	}
	public String getImageNormal() {
		return ImageNormal;
	}
	public void setImageNormal(String imageNormal) {
		ImageNormal = imageNormal;
	}
	public String getImageFocus() {
		return imageFocus;
	}
	public void setImageFocus(String imageFocus) {
		this.imageFocus = imageFocus;
	}
	public String getDisplayOrder() {
		return DisplayOrder;
	}
	public void setDisplayOrder(String displayOrder) {
		DisplayOrder = displayOrder;
	}
	public String getPackageName() {
		return PackageName;
	}
	public void setPackageName(String packageName) {
		PackageName = packageName;
	}
	
}

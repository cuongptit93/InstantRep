package jp.co.efusion.listhelper;

/**
 * Created by xor2 on 12/8/15.
 */
public class ThemeItem {

    private String title;
    private String image;

    public ThemeItem(String title,String image){
        this.title=title;
        this.image=image;
    }
    public String getTitle(){
        return this.title;

    }
    public String getImage(){
        return this.image;
    }

}

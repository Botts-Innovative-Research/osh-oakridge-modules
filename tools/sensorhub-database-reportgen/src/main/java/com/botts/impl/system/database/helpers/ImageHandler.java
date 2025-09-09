package com.botts.impl.system.database.helpers;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import java.io.File;

public class ImageHandler {
    Document document;

    public ImageHandler(Document document) {
        this.document = document;
    }

    public void addImage(String imgPath){
        try{
            File file = new File(imgPath);
            if(file.exists()){
                Image image = new Image(ImageDataFactory.create(imgPath));
                image.scaleToFit(500, 300);
                document.add(image);
                file.delete();
            }
        }catch(Exception e){
            System.out.println("Error while adding image to pdf");
        }
    }
}

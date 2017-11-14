package com.softjourn;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.fswebcam.FsWebcamDriver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class WebCamCommander {

    private Webcam webcam;

    public WebCamCommander() {
        Webcam.setDriver(new FsWebcamDriver());
        webcam = Webcam.getDefault();
    }

    public BufferedImage takePhoto(Integer width, Integer height) throws IOException {
        webcam.setCustomViewSizes(new Dimension[]{new Dimension(width, height)});
        webcam.setViewSize(new Dimension(width, height));
        webcam.open();
        BufferedImage image = webcam.getImage();
        webcam.close();
        return image;
    }
}

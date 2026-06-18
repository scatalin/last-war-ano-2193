package com.lastwar.ano2193.ocr;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

public class TesseractOcrStrategy implements OcrStrategy {

    private final String tessDataPath;

    public TesseractOcrStrategy(String tessDataPath) {
        this.tessDataPath = tessDataPath;
    }

    @Override
    public String extractText(File imageFile) throws OcrException {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        tesseract.setOcrEngineMode(1);  // LSTM neural-net mode
        tesseract.setPageSegMode(6);    // Assume a uniform block of text
        try {
            return tesseract.doOCR(imageFile);
        } catch (TesseractException e) {
            throw new OcrException("Tesseract failed on '" + imageFile.getName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return "tesseract";
    }
}
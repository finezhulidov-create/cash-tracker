package dev.zhulidov.cash_tracker.transactions.importpdf;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfImportService {

    public List<String> extractText(MultipartFile multipartFile){
        try (PDDocument document = Loader.loadPDF(multipartFile.getBytes())){
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            int pages = document.getNumberOfPages();
            List<String> allPages = new ArrayList<>();
            for (int page = 1; page <= pages ; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                log.info("Page added");
                allPages.add(pageText);
            }
            return allPages;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new InvalidRequestException("Поддержка только пдф файлов");
        }

    }


}

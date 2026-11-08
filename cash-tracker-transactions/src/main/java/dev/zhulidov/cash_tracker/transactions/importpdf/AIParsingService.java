package dev.zhulidov.cash_tracker.transactions.importpdf;



import dev.zhulidov.cash_tracker.transactions.dto.*;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.service.CategoryService;
import dev.zhulidov.cash_tracker.transactions.util.StatementValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIParsingService {


    private final AiParser parser;
    private final CategoryRepository categoryRepository;
    private final PdfImportService importService;
    private final CategoryService categoryService;
    private final DuplicateDetector detector;

    public List<ImportPreviewItem> generateTransactions(MultipartFile multipartFile, Long userId){
      List<TransactionCreateRequest> requests = new ArrayList<>();
      List<String> pages =  importService.extractText(multipartFile);
      List<ParsedOperations> result = new ArrayList<>();
      for (String page : pages){
            result.add(parser.parse(page));
        }
      List<ParsedOperations.OneOperation> operationsjson = new ArrayList<>();
      result.stream().filter(Objects::nonNull).map(ParsedOperations::operations)
              .filter(Objects::nonNull)
              .forEach(operations->{
                    operations.forEach(operation ->  {
                         if (operation != null){
                             operationsjson.add(operation);
                         }
                    });
              });
        Category defaultCat = categoryRepository.findFirstByUserIdAndCategoryName(userId,"Прочее")
                .orElseGet(()-> {
                    var cat = categoryService.createCategory(new CategoryCreateRequestDto("Прочее"),userId);
                    return Category.builder()
                            .id(cat.id())
                            .categoryName(cat.categoryName())
                            .userId(cat.userId())
                            .build();
                });

      for (ParsedOperations.OneOperation o:operationsjson){
          Optional<ParsedAmount> amount = StatementValues.parseAmount(o.amount());
          Optional<LocalDateTime> dateTime = StatementValues.parseDateTime(o.datetime());
          if (amount.isEmpty() || dateTime.isEmpty()){
              continue;
          }
          if (amount.get().income()){
              // в приложении только траты, поступления скипаем
              continue;
          }
          BigDecimal sum = amount.get().absolute();
          var request = new TransactionCreateRequest(cleanDescription(o.description()), sum,
                  dateTime.get(),List.of(new SplitRequest(defaultCat.getId(),sum)));

          requests.add(request);

      }
        log.debug("Created list of requests: {}", requests);
        return detector.markDupl(requests,userId);
    }


    private String cleanDescription(String description) {
        if (description != null) {
            String result = description.trim();
            if (result.isEmpty()) {
                String res = "Операция из выписки";
                return res;
            }
            result = result.length() > 255 ? result.substring(0, 254) : result;

            return result;
        }
        return "Операция из выписки";
    }






}

package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class PhotoForwarder implements MsgForwarder {
    private final SendPhoto sender = new SendPhoto();

    @Override public String getId(Update update) {
        return update.getMessage().getPhoto().stream()
                .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst()
                .orElse(null).getFileId();
    }

    @Override public void prepare(long targetId, Update update, InputFile inputFile) {
        sender.setChatId(targetId);
        sender.setCaption(update.getMessage().getCaption());
        sender.setPhoto(inputFile);
    }

    @Override public void forward(TelegramLongPollingBot bot) {
        try {
            bot.execute(sender);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override public String getText(Update update) {
        return "Photo with " + update.getMessage().getCaption();
    }
}

package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendVideoNote;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class VideoNoteForwarder implements Forwarder {
    private final SendVideoNote sender = new SendVideoNote();

    @Override public boolean hasMedia(Update update) {
        return update.getMessage().hasVideoNote();
    }

    @Override public String getId(Update update) {
        return update.getMessage().getVideoNote().getFileId();
    }

    @Override public void prepare(long targetId, Update update, InputFile inputFile) {
        sender.setChatId(targetId);
        sender.setVideoNote(inputFile);
    }

    @Override public void forward(TelegramLongPollingBot bot) {
        try {
            bot.execute(sender);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override public String getText(Update update) {
        return "VideoNote";
    }
}

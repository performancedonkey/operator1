package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class AudioForwarder implements Forwarder {
    private final SendAudio sender = new SendAudio();

    @Override public boolean hasMedia(Update update) {
        return update.getMessage().hasAudio();
    }

    @Override public String getId(Update update) {
        return update.getMessage().getAudio().getFileId();
    }

    @Override public void prepare(long targetId, Update update, InputFile inputFile) {
        sender.setChatId(targetId);
        sender.setCaption(update.getMessage().getCaption());
        sender.setAudio(inputFile);
    }

    @Override public void forward(TelegramLongPollingBot bot) throws TelegramApiException {
        bot.execute(sender);
    }

    @Override public String getText(Update update) {
        return "Audio";
    }
}

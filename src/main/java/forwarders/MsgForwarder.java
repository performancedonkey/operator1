package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public interface MsgForwarder {
    final GetFile getFile = new GetFile();

    final String mediaPath = "C:/tmp/TGBotMedia/";
    final String tgbot = "https://api.telegram.org/file/bot";

    default void forward(TelegramLongPollingBot bot, long fromId, long targetId, Update update) {
        if (targetId == 0) return;

        prepare(targetId, update, getFile(targetId, bot, update));
        forward(bot);
        log(fromId, targetId, getText(update));
    }

    default String getText(Update update) {
        return update.getMessage().getText();
    }

    default InputFile getFile(long targetId, TelegramLongPollingBot bot, Update update) {
        try {
            getFile.setFileId(getId(update));
            org.telegram.telegrambots.meta.api.objects.File file = bot.execute(getFile);
            String url = tgbot + bot.getBotToken() + "/" + file.getFilePath();

            String filepath = mediaPath + file.getFilePath();
            // Ensure directory exists
            new File(filepath).getParentFile().mkdirs();
            // Download file to local dir
            Files.copy(new URL(url).openStream(), Paths.get(filepath), StandardCopyOption.REPLACE_EXISTING);

            return new InputFile(new File(filepath));
        } catch (Exception e) {
            // e.printStackTrace(\);
        }
        return null;
    }

    default void log(long fromId, long targetId, String text) {
        System.out.println(fromId + " -> " + targetId + " = " + text);
    }

    public String getId(Update update);

    public void prepare(long targetId, Update update, InputFile inputFile);

    void forward(TelegramLongPollingBot bot);
}

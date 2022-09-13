package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class MessageForwarder implements MsgForwarder {
    private final SendMessage sender = new SendMessage();

    public MessageForwarder() {
        sender.enableHtml(true);
    }
    
    @Override public String getId(Update update) {
        return update.getUpdateId().toString();
    }

    @Override public void prepare(long targetId, Update update, InputFile inputFile) {
        sender.setChatId(targetId);
        sender.setText(update.getMessage().getText());
    }
    
    @Override public void forward(TelegramLongPollingBot bot) {
        try {
            bot.execute(sender);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendContact;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class ContactForwarder implements Forwarder {
    private final SendContact sender = new SendContact();

    @Override public boolean hasMedia(Update update) {
        return update.getMessage().hasContact();
    }

    @Override public String getId(Update update) {
        return update.getMessage().getContact().getUserId().toString();
    }

    @Override public void prepare(long targetId, Update update, InputFile inputFile) {
        sender.setChatId(targetId);
        sender.setFirstName(update.getMessage().getContact().getFirstName());
        sender.setLastName(update.getMessage().getContact().getLastName());
        sender.setPhoneNumber(update.getMessage().getContact().getPhoneNumber());
        sender.setVCard(update.getMessage().getContact().getVCard());
    }

    @Override public void forward(TelegramLongPollingBot bot) throws TelegramApiException {
        bot.execute(sender);
    }

    @Override public String getText(Update update) {
        return "Contact";
    }
}

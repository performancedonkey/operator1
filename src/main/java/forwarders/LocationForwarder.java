package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class LocationForwarder implements Forwarder {
    private final SendLocation sender = new SendLocation();

    @Override public boolean hasMedia(Update update) {
        return update.getMessage().hasLocation();
    }

    @Override public String getId(Update update) {
        return update.getMessage().getLocation().toString();
    }

    @Override public void prepare(long targetId, Update update, InputFile inputFile) {
        sender.setChatId(targetId);
        sender.setLatitude(update.getMessage().getLocation().getLatitude());
        sender.setLongitude(update.getMessage().getLocation().getLongitude());
        sender.setHeading(update.getMessage().getLocation().getHeading());
        sender.setHorizontalAccuracy(update.getMessage().getLocation().getHorizontalAccuracy());
        sender.setLivePeriod(update.getMessage().getLocation().getLivePeriod());
        sender.setProximityAlertRadius(update.getMessage().getLocation().getProximityAlertRadius());
    }

    @Override public void forward(TelegramLongPollingBot bot) throws TelegramApiException {
        bot.execute(sender);
    }

    @Override public String getText(Update update) {
        return "Location";
    }
}

package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-13
 */
public class MessageForwarder {

    private final List<Forwarder> forwarders = new ArrayList<>();

    public MessageForwarder() {
        forwarders.add(new TextForwarder());
        forwarders.add(new AudioForwarder());
        forwarders.add(new VoiceForwarder());
        forwarders.add(new PhotoForwarder());
        forwarders.add(new VideoNoteForwarder());
        forwarders.add(new LocationForwarder());
        forwarders.add(new DocumentForwarder());
        forwarders.add(new ContactForwarder());
        forwarders.add(new PollForwarder());
    }

    public void forward(TelegramLongPollingBot bot, long fromId, long targetId, Update update) {
        for (int i = 0; i < forwarders.size(); i++) {
            forwarders.get(i).forward(bot, fromId, targetId, update);
        }
    }
}

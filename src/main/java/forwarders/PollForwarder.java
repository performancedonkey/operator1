package forwarders;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendContact;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class PollForwarder implements Forwarder {
    private final SendPoll sender = new SendPoll();

    @Override public boolean hasMedia(Update update) {
        return update.getMessage().hasPoll();
    }

    @Override public String getId(Update update) {
        return update.getMessage().getPoll().getId();
    }

    @Override public void prepare(long targetId, Update update, InputFile inputFile) {
        sender.setChatId(targetId);
        sender.setAllowMultipleAnswers(update.getPoll().getAllowMultipleAnswers());
        sender.setCloseDate(update.getPoll().getCloseDate());
        sender.setCorrectOptionId(update.getPoll().getCorrectOptionId());
        sender.setExplanation(update.getPoll().getExplanation());
        sender.setExplanationEntities(update.getPoll().getExplanationEntities());
        sender.setIsAnonymous(update.getPoll().getIsAnonymous());
        sender.setType(update.getPoll().getType());
        sender.setOpenPeriod(update.getPoll().getOpenPeriod());
//        sender.setReplyToMessageId();
    }

    @Override public void forward(TelegramLongPollingBot bot) throws TelegramApiException {
        bot.execute(sender);
    }

    @Override public String getText(Update update) {
        return "Contact";
    }
}

import forwarders.*;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class OperatorBot extends TelegramLongPollingBot {
    @Override public String getBotUsername() {
        return "gurtenmati_bot";
    }

    @Override public String getBotToken() {
        return "5357387837:AAG8NhHJCreHh7N_uQFqF_MY-v3v6Lt8Sq4";
    }

    private final static HashMap<String, Long> allUsers = new HashMap<>();
    private final static Long2ObjectHashMap<String> allUsersById = new Long2ObjectHashMap<>();
    // Operator bot should be managing multiple hierarchies. all this should be abstracted

    // TODO this should be under one structure
    private final static Set<String> posts = new HashSet<>();
    private final static HashMap<String, Long> shifts = new HashMap<>();
    private final static Long2ObjectHashMap<String> who = new Long2ObjectHashMap<>();

    private final static Long2LongHashMap chatMapping = new Long2LongHashMap(0);

    static {
        posts.add("r");
        posts.add("q");
        posts.add("rad");
        posts.add("ped");
    }

    public void onUpdateReceived(Update update) {
//        long fromId = update.getMessage().getChatId();
        Long fromId = update.getMessage().getFrom().getId();
        String name = update.getMessage().getFrom().getFirstName() + update.getMessage().getFrom().getLastName();
        Integer msgId = update.getMessage().getMessageId();
        Integer updateId = update.getUpdateId();

        allUsers.put(name, fromId);
        allUsersById.put(fromId.longValue(), name);

        long targetId = chatMapping.getOrDefault(fromId, fromId);

        if (handleText(fromId, name, update.getMessage().getText(), msgId)) return;
        if (update.getMessage().hasText())
            forward(new MessageForwarder(), fromId, targetId, update);

        // Handle media
        if (update.getMessage().hasPhoto())
            forward(new PhotoForwarder(), fromId, targetId, update);

        if (update.getMessage().hasAudio())
            forward(new AudioForwarder(), fromId, targetId, update);

        if (update.getMessage().hasVoice())
            forward(new VoiceForwarder(), fromId, targetId, update);
    }

    private void forward(MsgForwarder forwarder, long fromId, long targetId, Update update) {
        forwarder.forward(this, fromId, targetId, update);
    }

    private boolean handleText(long fromId, String name, String text, Integer messageId) {
        if (text == null) return false;

        System.out.println(fromId + ": " + text);

        if (text.startsWith("Shift ")) {
            String position = text.split(" ")[1];
            if (position.equals("end")) {
                relieveFromWatch(fromId, fromId, "");
            } else if (validate(position, fromId)) {
                Long relieved = shifts.put(position, fromId);
                who.put(fromId, position);
                sendMessage(fromId, fromId, name + " started shift " + position);
                if (relieved != null)
                    relieveFromWatch(fromId, relieved, " Relieved by " + name);
            } else {
                sendMessage(fromId, fromId, "Available posts: " + posts.toString());
            }
        } else {
            String requestFrom = who.getOrDefault(fromId, name);
            if (text.startsWith("TT ")) {
                String talkTo = text.split(" ")[1];
                Long targetId = shifts.get(talkTo);
                if (targetId == null) {
                    targetId = allUsers.get(talkTo);
                }
                if (targetId != null) {
                    long oldValue = chatMapping.put(targetId.longValue(), fromId);
//                    if (oldValue == 0) {
                    sendMessage(fromId, fromId, "You are now connected to " + talkTo);
                    sendMessage(fromId, targetId, requestFrom + " has requested to talk to " + talkTo);
                    chatMapping.put(fromId, targetId.longValue());
//                    }
                } else {
                    text = "Can't connect to " + talkTo + ". Available: " + shifts.keySet();
                    sendMessage(fromId, fromId, text);
                }
            } else if (text.equals("CC")) {
                Executors.newSingleThreadExecutor().execute(() -> clearChat(fromId));
            } else if (text.equals("/start")) {
                text = "Welcome " + name + ". Your id is:" + fromId;

                sendMessage(fromId, fromId, text);
            } else if (text.equals("END")) {
                long targetChatId = chatMapping.remove(fromId);

                sendMessage(fromId, fromId, "Disconnected from " + targetChatId);
                sendMessage(fromId, targetChatId, "Disconnected from " + fromId);
            } else if (chatMapping.containsKey(fromId)) {
                // relay message
                long targetChatId = chatMapping.get(fromId);

                sendMessage(fromId, targetChatId, messageId + " | " + requestFrom + ": " + text);
            } else {
                text = "Hey " + requestFrom + "! Try taking a <b>Shift</b> or <b>TT</b>";
                sendMessage(fromId, fromId, text);
            }
        }
        return true;
    }

    private void relieveFromWatch(long fromId, long relievedId, String text) {
        String pos = who.remove(relievedId);
        shifts.remove(pos);
        sendMessage(fromId, relievedId, allUsersById.get(relievedId) + " your watch has ended: " + pos + ". " + text);
    }

    private final SendMessage response = new SendMessage();
    private void sendMessage(long fromId, long targetId, String text) {
        response.enableHtml(true);

        response.setChatId(targetId);

        response.setText(text);

        try {
            execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        System.out.println(fromId + " -> " + targetId + " = " + text);
    }

    private boolean validate(String position, long chatId) {
        if (!posts.contains(position)) return false;
        return shifts.getOrDefault(position, 0L) != chatId;
    }

    private void clearChat(long chatId) {
        DeleteMessage delete = new DeleteMessage();
        delete.setChatId(chatId);
        for (int i = 0; i < 1000; i++) {
            delete.setMessageId(i);
            try {
                execute(delete);
            } catch (TelegramApiException e) {
            }
        }
    }
}

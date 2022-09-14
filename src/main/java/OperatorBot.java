import forwarders.MessageForwarder;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA.
 * User: Mati
 * Date: 2022-09-12
 */
public class OperatorBot extends TelegramLongPollingBot {
    private final int BOTID = 0;

    @Override public String getBotUsername() {
        return "gurtenmati_bot";
    }

    @Override public String getBotToken() {
        return "5357387837:AAG8NhHJCreHh7N_uQFqF_MY-v3v6Lt8Sq4";
    }

    private final static HashMap<String, Long> userIds = new HashMap<>();
    private final static Long2ObjectHashMap<String> usersById = new Long2ObjectHashMap<>();
    // Operator bot should be managing multiple hierarchies. all this should be abstracted

    // TODO this should be under one structure
    private final static Set<String> posts = new HashSet<>();
    private final static HashMap<String, Long> shifts = new HashMap<>();
    private final static Long2ObjectHashMap<String> onCall = new Long2ObjectHashMap<>();

    private final static Long2LongHashMap chatMapping = new Long2LongHashMap(0);

    static {
        posts.add("r");
        posts.add("q");
        posts.add("rad");
        posts.add("ped");
    }

    private final MessageForwarder forwarder = new MessageForwarder();
    public void onUpdateReceived(Update update) {
        try {
            //        long fromId = update.getMessage().getChatId();
            Long fromId = update.getMessage().getFrom().getId();
            String name = update.getMessage().getFrom().getFirstName() + update.getMessage().getFrom().getLastName();
            Integer msgId = update.getMessage().getMessageId();
            Integer updateId = update.getUpdateId();

            userIds.put(name, fromId);
            usersById.put(fromId.longValue(), name);

            parseCommands(fromId, update, msgId);

            long targetId = chatMapping.getOrDefault(fromId, fromId);
            boolean isConnected = chatMapping.containsKey(fromId);

            forwarder.forward(this, fromId, targetId, update);
        } catch (Exception e) {
            System.out.println("here");
        }
    }

    private void parseCommands(long fromId, Update update, Integer messageId) {
        String text = update.getMessage().getText();
        if (text == null) return;
        String name = usersById.getOrDefault(fromId, "" + fromId);
        System.out.println(fromId + " <- " + text);
        String command = text.replace(" ", "_");

        String currentPost = onCall.get(fromId);
        if (command.equals("#CC")) {
            Executors.newSingleThreadExecutor().execute(() -> clearChat(fromId));
        } else if (command.equals("/start")) {
            sendAdministrative(fromId, update, "Welcome " + name + ". Your id is: " + fromId
                    + " and your pos is " + currentPost
                    + " and you are connected to " + getConnectedTo(fromId));
        } else if (command.startsWith("/Shift")) {
            command = command.replace(" ", "_");
            String newPost = (command + "  ").substring("/Shift_".length()).trim();
            if (newPost.equals("end")) {
                relieveFromWatch(fromId, update, currentPost);
            } else if (validate(newPost, fromId)) {
                if (currentPost != null && !newPost.equals(currentPost)) {
                    relieveFromWatch(fromId, update, currentPost);//, " Relieved by " + name);
                }
                Long relieved = shifts.put(newPost, fromId);
                String shifted = onCall.get(fromId);
                if (relieved != null)
                    relieveFromWatch(relieved, update, newPost);//, " Relieved by " + name);
                onCall.put(fromId, newPost);
                sendAdministrative(fromId, update, name + (shifted == null ? " started shift " : " shifted from " + shifted + " to ") + newPost);
            } else {
                sendAdministrative(fromId, update, "Available posts: " +
                        posts.stream().map(s -> "\n/Shift_" + s).collect(Collectors.toList()));
            }
        } else if (command.startsWith("/TT")) {
            command = command.replace(" ", "_");
            String talkTo = (command + "  ").substring("/TT_".length()).trim();
            Long targetId = shifts.containsKey(talkTo) ? shifts.get(talkTo) : userIds.get(talkTo);
            if (targetId != null) {
                if (targetId == fromId) {
                    sendAdministrative(fromId, update, "Fool, you cant connect to yourself!");
                } else {
                    long oldChat = chatMapping.put(targetId.longValue(), fromId);
                    sendAdministrative(fromId, update, "You are now connected to " + talkTo);
                    chatMapping.put(fromId, targetId.longValue());
                    sendAdministrative(targetId, update, name + " has requested to talk to " + talkTo);
                    if (oldChat != fromId) {
                        sendAdministrative(oldChat, update, name + " has disconnected you from " + talkTo);
                        chatMapping.remove(oldChat);
                    }
                }
            } else {
                sendAdministrative(fromId, update, "No " + talkTo + " available. Try " +
                        shifts.keySet().stream().map(s -> "\n/TT_" + s).collect(Collectors.toList()));
            }
        } else if (command.equalsIgnoreCase("END")) {
            long targetChatId = chatMapping.remove(fromId);
            sendAdministrative(fromId, update, "Disconnected from " + usersById.get(targetChatId));
            chatMapping.remove(targetChatId);
            sendAdministrative(targetChatId, update, usersById.get(fromId) + " terminated the connection");
        } else if (!chatMapping.containsKey(fromId)) {
            sendAdministrative(fromId, update, "Hey " + name + "! Try taking a <b>/Shift</b> or <b>/TT</b>");
        } else {
            // relay message
            String header = "<i>" + name + " (" + messageId + ")</i> - ";
            update.getMessage().setText(header + text);
        }
    }

    private String getConnectedTo(long userId) {
        long targetId = chatMapping.get(userId);
        return targetId == 0 ? "" : usersById.get(targetId);
    }

    private void relieveFromWatch(long relievedId, Update update, String position) {
        onCall.remove(relievedId);
        shifts.remove(position);
        sendAdministrative(relievedId, update, usersById.get(relievedId) + " your watch has ended: " + position + ". ");
    }

    private final MessageForwarder administrative = new MessageForwarder();
    private void sendAdministrative(long targetId, Update update, String text) {
        if (text == null) return;

        String oldText = update.getMessage().getText();
        update.getMessage().setText(text);
        administrative.forward(this, BOTID, targetId, update);
        update.getMessage().setText(oldText);
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

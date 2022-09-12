import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;
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
        long chatId = update.getMessage().getChatId();
        String name = update.getMessage().getFrom().getFirstName() + update.getMessage().getFrom().getLastName();
        Integer msgId = update.getMessage().getMessageId();

        allUsers.put(name, chatId);
        allUsersById.put(chatId, name);

        if (!handleText(chatId, name, update.getMessage().getText(), msgId)) {
            // handle other stuff. handle media.
//            System.out.println(update.getUpdateId());
            if (update.getMessage().hasPhoto()) {
                savePhoto(chatId, photo_test_path);
//                sendPhoto(chatId, update);
            }
        }
    }

    private boolean handleText(long chatId, String name, String text, Integer messageId) {
        if (text == null) return false;
        System.out.println(chatId + ": " + text);

        if (text.startsWith("Shift ")) {
            String position = text.split(" ")[1];
            if (position.equals("end")) {
                relieveFromWatch(chatId, "");
            } else if (validate(position, chatId)) {
                Long relieved = shifts.put(position, chatId);
                who.put(chatId, position);
                sendMessage(chatId, name + " started shift " + position);
                if (relieved != null)
                    relieveFromWatch(relieved, " Relieved by " + name);
            } else {
                sendMessage(chatId, "Available posts: " + posts.toString());
            }
        } else {
            String requestFrom = who.getOrDefault(chatId, name);
            if (text.startsWith("TT ")) {
                String talkTo = text.split(" ")[1];
                Long targetChat = shifts.get(talkTo);
                if (targetChat == null) {
                    targetChat = allUsers.get(talkTo);
                }
                if (targetChat != null) {
                    long oldValue = chatMapping.put(targetChat.longValue(), chatId);
                    if (oldValue == 0) {
                        sendMessage(chatId, "You are now connected to " + talkTo);
                        sendMessage(targetChat, "You are now connected to " + requestFrom);
                    }
                    chatMapping.put(chatId, targetChat.longValue());
                } else {
                    text = "Can't connect to " + talkTo + ". Available: " + shifts.keySet();
                    sendMessage(chatId, text);
                }
            } else if (text.equals("CC")) {
                Executors.newSingleThreadExecutor().execute(() -> clearChat(chatId));
            } else if (text.equals("/start")) {
                text = "Welcome " + name + ". Your id is:" + chatId;

                sendMessage(chatId, text);
            } else if (text.equals("END")) {
                long targetChatId = chatMapping.remove(chatId);

                sendMessage(chatId, "Disconnected from " + targetChatId);
                sendMessage(targetChatId, "Disconnected from " + chatId);
            } else if (chatMapping.containsKey(chatId)) {
                // relay message
                long targetChatId = chatMapping.get(chatId);

                sendMessage(targetChatId, messageId + " | " + requestFrom + ": " + text);
            } else {
                text = "Hey " + requestFrom + "! Try taking a <b>Shift</b> or <b>TT</b>";
                sendMessage(chatId, text);
            }
        }
        return true;
    }

    private void relieveFromWatch(long chatId, String text) {
        String pos = who.remove(chatId);
        shifts.remove(pos);
        sendMessage(chatId, allUsersById.get(chatId) + " your watch has ended: " + pos + ". " + text);
    }

    private boolean validate(String position, long chatId) {
        if (!posts.contains(position)) return false;
        return shifts.getOrDefault(position, 0L) != chatId;
    }


//    private void sendPhoto(long targetChatId, Update update) {
////        File mediaFile = savePhoto(update);
////        if (mediaFile == null) return;
//
//        photoRelay.setChatId(targetChatId);
//        InputFile photo = new InputFile(mediaFile);
//        photoRelay.setPhoto(photo);
//
//        try {
//            execute(photoRelay);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }

    GetFile getFile = new GetFile();
    private static final String photo_test_path = "C:\\Processed\\Pictures\\Received\\WebImages\\PIC_20200928_085402_CameraUploads.jpg";
    private final SendPhoto sendPhoto = new SendPhoto();
    private void savePhoto(long chatId, String photopath) {
//        List<PhotoSize> photos = update.getMessage().getPhoto();
//        String f_id = photos.stream()
//                .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
//                .findFirst()
//                .orElse(null).getFileId();

        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(new File(photopath)));
        sendPhoto.setCaption("A performant Donkey");
        try {
            execute(sendPhoto); // Call method to send the photo with caption
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
//
//        String uploadedFileId = update.getMessage().getPhoto().get(0).getFileId();
//        getFile.setFileId(uploadedFileId);
//
//        try {
//            File file = execute(getFile);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//
//        InputStream inputStream = new FileInputStream(file);

        //        String uploadedFilePath = getFile(getFile).getFilePath();
        //        File localFile = new File("localPath/filename.doc");
        //        InputStream is = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + uploadedFilePath).openStream();
        //        FileUtils.copyInputStreamToFile(is, localFile);
//        return new InputFile();
    }

    private File savePhoto(Update update) {
        if (update.getMessage().hasDocument()) {
            String doc_id = update.getMessage().getDocument().getFileId();
            String doc_name = update.getMessage().getDocument().getFileName();
            String doc_mine = update.getMessage().getDocument().getMimeType();
            long doc_size = update.getMessage().getDocument().getFileSize();
            String getID = String.valueOf(update.getMessage().getFrom().getId());

            Document document = new Document();
            document.setMimeType(doc_mine);
            document.setFileName(doc_name);
            document.setFileSize(doc_size);
            document.setFileId(doc_id);

            GetFile getFile = new GetFile();
            getFile.setFileId(document.getFileId());
            try {
                org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
                return downloadFile(file, new File("./data/userDoc/" + getID + "_" + doc_name));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private final SendMessage response = new SendMessage();
    private void sendMessage(long targetChatId, String text) {
        response.enableHtml(true);

        response.setChatId(targetChatId);

        response.setText(text);

//        response.setEntities(update.getMessage().getEntities());
        try {
            execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        System.out.println(text);
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

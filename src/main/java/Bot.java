import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import static java.util.Collections.min;
import static org.telegram.telegrambots.logging.BotLogger.log;

public class Bot extends TelegramLongPollingBot {

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        sendStartMsg(update, update.getMessage().getChatId().toString(), "Начал следить за билетами!");
        scheduler(update.getMessage().getChatId().toString());
    }

    /**
     * Метод для настройки сообщения и его отправки.
     * @param chatId id чата
     * @param s Строка, которую необходимот отправить в качестве сообщения.
     */
    private synchronized void sendMsg(String chatId, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        setInlineMarkup(sendMessage);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            log(Level.SEVERE, "Exception: ", e.toString());
        }
    }

    private synchronized void sendStartMsg(Update update, String chatId, String messageText) {
        if (update.getMessage().getText().equals("/start")) {
            SendMessage message = new SendMessage()
                    .setChatId(chatId)
                    .setText(messageText);
            try {
                execute(message);

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void setInlineMarkup(SendMessage sendMessage) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        rowInline.add(new InlineKeyboardButton().setText("Купить билет!").setUrl("https://www1.ticketmaster.com/wwe-nxt-takeover-new-york/event/00005574C9655B3D"));
        // Set the keyboard to the markup
        rowsInline.add(rowInline);
        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(markupInline);
    }

    @Override
    public String getBotUsername() {
        return "NXT Ticket Bot";
    }

    @Override
    public String getBotToken() {
        return "772768624:AAEGpc9nb7AuXJcGTdpGS3ntJq3ieuylL1Y";
    }

    private void scheduler(String chatId) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkTicketPrice(chatId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 600000);
    }

    private void checkTicketPrice(String myChatId) throws IOException {
        HttpUriRequest request = new HttpGet( "https://services.ticketmaster.com/api/ismds/event/00005574C9655B3D/facets?show=listpricerange&by=offers&oq=not(locked)&q=available&apikey=b462oi7fic6pehcdkzony5bxhe&apisecret=pquzpfrfz7zd2ylvtz3w5dtyse&resaleChannelId=internal.ecommerce.consumer.desktop.web.browser.ticketmaster.us" );
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

        String json = IOUtils.toString(httpResponse.getEntity().getContent());
        System.out.println(json);
        json = "[" + json + "]";

        JSONArray jsonArray = new JSONArray(new JSONTokener(json));
        List<Integer> priceList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            int lengthTypesOfTickets = object.getJSONArray("facets").length();
            for (int indexOfFacets = 0; indexOfFacets < lengthTypesOfTickets; indexOfFacets++) {
                JSONArray newJsonArray = object.getJSONArray("facets").getJSONObject(indexOfFacets).getJSONArray("listPriceRange");
                int obj = newJsonArray.getJSONObject(0).getInt("min");
                priceList.add(obj);
            }
            System.out.println(priceList);
        }

        if (min(priceList) < 136) {
            sendMsg(myChatId, "Билеты подешевели!!! Новая низкая цена:" + min(priceList) +"$");
        }
    }
}
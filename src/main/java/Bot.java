import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class Bot extends TelegramLongPollingBot {

    /*
     Здесь происходит регистрация АПИ,
     создается как таковой объект телеграм апи,
     а также регистрируется бот
    */
    static String botToken = "1302392610:AAFOJ_UCYOH6OJzm17VN0lykEaFg14JFXN4";
    static HashMap<String, String> user2City = new HashMap<>();
    static List<String> userAlertsList = new ArrayList<String>();

    public static void main(String[] args) {

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        //Запускаем метод оповещения пользоватеелй по подписке в async отдельно
        CompletableFuture.runAsync(() -> {
            try {
                alerts();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        try {
             telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }




    //подписка-отписка
    public void userSubscribe(String newUser) {
        userAlertsList.add(newUser);
    }
    public void userUnsubscribe(String newUser){
        userAlertsList.remove(newUser);
    }

    //Оповещение пользователей
    public static void alerts() throws InterruptedException, IOException {

        Model model = new Model();
        while (true) {


            Calendar calendar = GregorianCalendar.getInstance();
            int hourNow = calendar.get(Calendar.HOUR_OF_DAY);
            int minuteNow = calendar.get(Calendar.MINUTE);

            //Время оповещения
            if ((hourNow == 7) && (minuteNow == 30)) {

                //Цикл по каждому пользователю
                for (int i = 0; i < userAlertsList.size(); i++) {

                    String userId = userAlertsList.get(i);
                    String currentUserCity = user2City.get(userId);
                    String weatherString = Weather.getWeatherDay(currentUserCity, model);

                    TelegramAlertURL(userId, weatherString);
                    System.out.println("Отправили сообщение по подписке пользователю " + userId);
                }

                //Спим 1 мин, чтоб снова не попасть в условие по времени оповещения
                Thread.sleep(60000);

            }
            Thread.sleep(10000);
        }
    }

    //Оповещение пользователей по URL
    public static String TelegramAlertURL(String userId, String messageText) throws IOException {

        String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
        String text = URLEncoder.encode(messageText, StandardCharsets.UTF_8);
        urlString = String.format(urlString, botToken, userId, text);
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        StringBuilder sb = new StringBuilder();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String inputLine = "";
        while ((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        String response = sb.toString();
        return response;

    }

    //Отправка сообщения
    public void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(text);
        sendMessage.setChatId(message.getChatId().toString());
        try {
            setButtons(sendMessage);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /*
     Тут мы инициализируем клаву, настраиваем разметку клавы,
     Выводим клавиатуру для всех пользователей
     Подгоняем клавиатуру под количество кнопок
     Делаем так, чтобы клава не скрывалась после нажатия кнопки
     И, соответственно, записываем наши кноки в строчки
     Можно вообще-то под каждую кнопку заводить строку, но я все в двух делаю
     Потом мы все строчки клавиатуры закидываем в список и уже его устанавливаем на клаве
    */
    public void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(); // создал клавиатуру
        sendMessage.setReplyMarkup(replyKeyboardMarkup); // разметка клавиатуры
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboardRowList = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        KeyboardRow keyboardSecondRow = new KeyboardRow();
        KeyboardRow keyboardThirdRow = new KeyboardRow();
        KeyboardRow keyboardFourthRow = new KeyboardRow();

        keyboardFirstRow.add(new KeyboardButton("Приветик ♥"));
        keyboardFirstRow.add(new KeyboardButton("Помоги :("));
        keyboardSecondRow.add(new KeyboardButton("На сутки"));
        keyboardSecondRow.add(new KeyboardButton("Сейчас"));
        keyboardThirdRow.add(new KeyboardButton("Москва"));
        keyboardThirdRow.add(new KeyboardButton("Владикавказ"));
        keyboardFourthRow.add(new KeyboardButton("Подписка"));
        keyboardFourthRow.add(new KeyboardButton("Отписка"));


        keyboardRowList.add(keyboardFirstRow);
        keyboardRowList.add(keyboardSecondRow);
        keyboardRowList.add(keyboardThirdRow);
        keyboardRowList.add(keyboardFourthRow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
    }

    /*
    Здесь мы принимаем обновления
    Создаем шаблоны ответов на определенные сообщения и устраняем возможные ошибки
    */
    public void onUpdateReceived(Update update) {
        Model model = new Model();
        Message message = update.getMessage();
        if (update.hasMessage()) {
            if (message != null && message.hasText()) {


                switch (message.getText()) {
                    case "Приветик ♥":
                        sendMsg(message, "Привет-привееет! ♥" +
                                "\n" + "Напиши название города, а я отправлю погоду)");
                        break;
                    case "Помоги :(":
                        sendMsg(message, "Этот бот нужен для того, чтобы подсказать тебе погоду. " +
                                "Для этого нужно ввести название города. " +
                                "Дальше бот сделает все сам :)");
                        break;
                    case "Подписка":

                        //if (user2City.containsKey(message.getChatId().toString())) {
                        //    userSubscribe(message.getChatId().toString());
                        //    sendMsg(message, "Успешно подписались на рассылку");
                        if (!user2City.containsKey(message.getChatId().toString())) {
                            sendMsg(message, "Вам необходимо сначала отправить ваш город");
                        }
                        else if (!userAlertsList.contains(message.getChatId().toString())){
                            userSubscribe(message.getChatId().toString());
                            sendMsg(message, "Успешно подписались на рассылку");
                        }
                        else if (userAlertsList.contains(message.getChatId().toString())){
                            sendMsg(message, "Вы уже подписаны на рассылку");
                        }
                        else{
                            sendMsg(message, "Что-то пошло не так");
                        }
                        break;
                    case "Отписка":
                        //if (user2City.containsKey(message.getChatId().toString())) {
                        //    userUnsubscribe(message.getChatId().toString());
                        //    sendMsg(message, "Успешно отписались от рассылки");
                        if (userAlertsList.contains(message.getChatId().toString())) {
                            userUnsubscribe(message.getChatId().toString());
                            sendMsg(message, "Успешно отписались от рассылки");
                        }
                        else{
                            sendMsg(message, "Вы еще не подписались");
                        }
                        break;
                    case "На сутки":
                        if (user2City.containsKey(message.getChatId().toString())) {
                            String userId = message.getChatId().toString();
                            String currentUserCity = user2City.get(userId);
                            try {
                                sendMsg(message, Weather.getWeatherDay(currentUserCity, model));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        else{
                            sendMsg(message, "Для начала введите ваш город");
                        }
                        break;

                    case "Сейчас":
                        if (user2City.containsKey(message.getChatId().toString())) {
                            String userId = message.getChatId().toString();
                            String currentUserCity = user2City.get(userId);
                            try {
                                sendMsg(message, Weather.getWeatherNow(currentUserCity, model));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        else{
                            sendMsg(message, "Для начала введите ваш город");
                        }
                        break;

                        //Обработка города
                    default:
                        try {
                            sendMsg(message, Weather.getWeatherDay(message.getText(), model));
                            user2City.put(message.getChatId().toString(),message.getText());
                        } catch (IOException e) {

                        }
                        catch (RuntimeException e){
                            sendMsg(message, "Что-то пошло не так... Не найдено.");
                        }
                        break;
                }
            }
        } else if (update.hasCallbackQuery()) {
            try {
                execute(new SendMessage().setText(update.getCallbackQuery().getData()).setChatId(update.
                        getCallbackQuery().getMessage().getChatId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    //Имя бота
    @Override
    public String getBotUsername() {return "FromOlga_WeatherBot";}
    //Токен бота

    @Override
    public String getBotToken() {
        return botToken;
    }
}
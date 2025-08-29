package ru.project_bot.finance.bot;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.project_bot.finance.bot.handler.CallbackQueryHandler;
import ru.project_bot.finance.bot.handler.MessageHandler;
import ru.project_bot.finance.bot.service.TelegramSenderService;

@Service
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    @Autowired
    private MessageHandler messageHandler;

    @Autowired
    private CallbackQueryHandler callbackQueryHandler;

    @Autowired
    private TelegramSenderService telegramSenderService;

    @Value("${bot.token}")
    @Getter
    private String botToken;

    @Value("${bot.username}")
    @Getter
    private String botUsername;

    private boolean botRegistered = false;

    public TelegramBot(@Value("${bot.token}") String botToken) {
        super(botToken);
    }

    @PostConstruct
    void initBot() {
        telegramSenderService.setTelegramBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                messageHandler.handleMessage(chatId, messageText);
            } else if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                int messageId = update.getCallbackQuery().getMessage().getMessageId();

                callbackQueryHandler.handleCallbackQuery(chatId, messageId, callbackData, update);
            }
        } catch (Exception e) {
            logger.error("Error handling onUpdateReceived: ", e);
        }
    }

    @EventListener(ContextRefreshedEvent.class)
    public void registerBot() {
        if (!botRegistered) {
            try {
                logger.info("🔄 Attempting to register bot for long polling...");
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(this);
                botRegistered = true;
                logger.info("✅ Bot successfully registered for long polling!");
                logger.info("📡 Long polling should start now...");
            } catch (TelegramApiRequestException e) {
                logger.error("❌ Telegram API request error during bot registration", e);
                if (e.getErrorCode() == 401) {
                    logger.error("🔑 Invalid bot token! Please check your BOT_TOKEN environment variable.");
                }
            } catch (TelegramApiException e) {
                logger.error("❌ Telegram API error during bot registration", e);
            } catch (Exception e) {
                logger.error("❌ Unexpected error during bot registration", e);
            }
        }
    }

}

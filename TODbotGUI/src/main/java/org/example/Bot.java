package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import org.example.features.Discord.TwitchToDiscordInteractions;
import org.example.features.Twitch.Games.Giveaway;
import org.example.features.Twitch.Games.Moods;
import org.example.features.Twitch.Games.QuoteSystem;
import org.example.features.Twitch.Games.TimedMessages;
import org.example.features.Twitch.Notifications.ChannelMessageOnClip;
import org.example.features.Twitch.Notifications.ChannelNotificationOnFollow;
import org.example.features.Twitch.Notifications.ChannelNotificationOnLive;
import org.example.features.Twitch.Notifications.ChannelNotificationOnSubscription;
import org.example.features.Twitch.Systems.CommandReplySystem;
import org.example.features.Twitch.Systems.EventLoggerToText;
import org.example.features.Twitch.Systems.WriteChannelChatToConsole;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class Bot {

    /**
     * Holds the Bot Configuration
     */
    private Configuration configuration;

    /**
     * Twitch4J API
     */
    private ITwitchClient twitchClient;



    /**
     * Constructor
     */
    public Bot() {
        // Load Configuration
        loadConfiguration();

        TwitchClientBuilder clientBuilder = TwitchClientBuilder.builder();

        //region Auth
        OAuth2Credential credential = new OAuth2Credential(
                "twitch",
                configuration.getCredentials().get("irc")
        );
        //endregion

        //region TwitchClient
        twitchClient = clientBuilder
                .withClientId(configuration.getApi().get("twitch_client_id"))
                .withClientSecret(configuration.getApi().get("twitch_client_secret"))
                .withEnableHelix(true)
                /*
                 * Chat Module
                 * Joins irc and triggers all chat based events (viewer join/leave/sub/bits/gifted subs/...)
                 */
                .withChatAccount(credential)
                .withEnableChat(true)
                .withEnablePubSub(true)
                /*
                 * Build the TwitchClient Instance
                 */
                .build();
        //endregion
    }

    /**
     * Method to register all features
     */
    public void registerFeatures() throws IOException, URISyntaxException {
        SimpleEventHandler eventHandler = twitchClient.getEventManager().getEventHandler(SimpleEventHandler.class);


        // Register Event-based features
        ChannelNotificationOnLive channelNotificationOnLive = new ChannelNotificationOnLive(this, eventHandler);
        ChannelNotificationOnFollow channelNotificationOnFollow = new ChannelNotificationOnFollow(eventHandler);
        ChannelNotificationOnSubscription channelNotificationOnSubscription = new ChannelNotificationOnSubscription(eventHandler);
        System.out.println("Basic features loaded");


        WriteChannelChatToConsole writeChannelChatToConsole = new WriteChannelChatToConsole(eventHandler);
        System.out.println("Console Logger Loaded");


        CommandReplySystem commandReplySystem = new CommandReplySystem(eventHandler,this);
        QuoteSystem quoteSystem = new QuoteSystem(eventHandler,this);
        EventLoggerToText eventLoggerToText = new EventLoggerToText(eventHandler,this);
        System.out.println("Expanded features Loaded");


        Giveaway giveaway = new Giveaway(eventHandler,this);
        TimedMessages timedMessages = new TimedMessages(eventHandler, this);
        Moods moods = new Moods(this,eventHandler);
        TwitchToDiscordInteractions twitchToDiscordInteractions = new TwitchToDiscordInteractions(this, eventHandler, commandReplySystem);
        ChannelMessageOnClip CMOP = new ChannelMessageOnClip(this, eventHandler);
        System.out.println("Twitch To Discord Features loaded");



    }

    /**
     * Load the Configuration
     */
    private void loadConfiguration() {
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream("config.yaml");

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            configuration = mapper.readValue(is, Configuration.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Unable to load Configuration ... Exiting.");
            System.exit(1);
        }
    }

    public void start() {
        // Connect to all channels
        for (String channel : configuration.getChannels()) {
            twitchClient.getChat().joinChannel(channel);

        }
        System.out.println("Joined");
        // Enable client helper for Stream GoLive / GoOffline / GameChange / TitleChange Events
        twitchClient.getClientHelper().enableStreamEventListener(configuration.getChannels());
        // Enable client helper for Follow Event
        twitchClient.getClientHelper().enableFollowEventListener(configuration.getChannels());
        twitchClient.getPubSub().listenForFollowingEvents(new OAuth2Credential(
                "twitch",
                configuration.getCredentials().get("irc")),configuration.getChannels().get(0));
        this.getTwitchClient().getChat().sendMessage("TheLostWielder","TODbot Initialized. Welcome to Twitch chat! How may I compute for you today?");
    }

    public ITwitchClient getTwitchClient() {
        return twitchClient;
    }
}
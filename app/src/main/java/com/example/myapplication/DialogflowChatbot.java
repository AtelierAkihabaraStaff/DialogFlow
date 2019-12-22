package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.object.conversation.BaseChatbot;
import com.aldebaran.qi.sdk.object.conversation.BaseChatbotReaction;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.ReplyPriority;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.SpeechEngine;
import com.aldebaran.qi.sdk.object.conversation.StandardReplyReaction;
import com.aldebaran.qi.sdk.object.locale.Locale;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

import static com.aldebaran.qi.sdk.object.conversation.ReplyPriority.FALLBACK;

public class DialogflowChatbot extends BaseChatbot {
    private static final String TAG = "DialogflowChatbot";
    private static final String access_token = "";
    private final QiContext context;

    protected DialogflowChatbot(QiContext context) {
        super(context);
        this.context = context;
    }

    @Override
    public StandardReplyReaction replyTo(Phrase phrase, Locale locale) {

        if(phrase.getText().isEmpty()) {
            return new StandardReplyReaction(
                    new MyChatbotReaction(getQiContext(), "聞き取れませんでした。"),
                    ReplyPriority.NORMAL);
        }
        if(phrase.getText().equals("<...>")){
            return new StandardReplyReaction(
                    new MyChatbotReaction(getQiContext(), "聞き取れませんでした。"),
                    ReplyPriority.NORMAL);
        }
        else {
            final AIConfiguration config = new AIConfiguration(access_token,
                    AIConfiguration.SupportedLanguages.Japanese,
                    AIConfiguration.RecognitionEngine.System);
            final AIDataService aiDataService = new AIDataService(context,config);
            String heard = phrase.getText();
            final AIRequest aiRequest = new AIRequest();
            aiRequest.setQuery(heard);
            RequestTask task = new RequestTask(aiDataService);
            task.execute(aiRequest);
            AIResponse response = task.getResponse();
            return replyFromAIResponse(response);
        }
    }

    private StandardReplyReaction replyFromAIResponse(final AIResponse response) {
        Log.d(TAG, "replyFromAIResponse");
        final Result result = response.getResult();
        String answer       = result.getFulfillment().getSpeech();
        Log.d(TAG, "result:"+result);
        Log.d(TAG,"answer:"+answer);

        return new StandardReplyReaction(
                new MyChatbotReaction(getQiContext(), answer),
                ReplyPriority.NORMAL);
    }
    class MyChatbotReaction extends BaseChatbotReaction {
        private String answer;
        private Future<Void> fSay;
        MyChatbotReaction(final QiContext context, String answer) {
            super(context);
            this.answer = answer;
        }
        @Override
        public void runWith(SpeechEngine speechEngine) {
            Say say = SayBuilder.with(speechEngine)
                    .withText(answer)
                    .build();
            fSay = say.async().run();
            try {
                fSay.get(); // Do not leave the method before the actions are done
            } catch (ExecutionException e) {
                Log.e(TAG, "Error during Say", e);
            } catch (CancellationException e) {
                Log.i(TAG, "Interruption during Say");
            }
        }
        @Override
        public void stop() {
            if (fSay != null) {
                fSay.cancel(true);
            }
        }
    }
}

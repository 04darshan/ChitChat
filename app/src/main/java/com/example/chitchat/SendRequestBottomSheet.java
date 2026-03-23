package com.example.chitchat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * SendRequestBottomSheet
 *
 * A Material3 BottomSheetDialogFragment shown when the user taps "Connect".
 * They can add an optional personal note (max 200 chars) before sending.
 *
 * Shows:
 *   - Target user's profile photo + name
 *   - Mutual friend count
 *   - Optional message field with character counter
 *   - Send button
 *
 * Usage:
 *   SendRequestBottomSheet sheet = SendRequestBottomSheet.newInstance(
 *       uid, username, photoUrl, mutualCount);
 *   sheet.setOnRequestSentListener(success -> { ... });
 *   sheet.show(getSupportFragmentManager(), "send_request");
 *
 * Add to AndroidManifest: no change needed (fragment, not activity).
 */
public class SendRequestBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_UID      = "uid";
    private static final String ARG_NAME     = "name";
    private static final String ARG_PHOTO    = "photo";
    private static final String ARG_MUTUALS  = "mutuals";
    private static final int    MAX_CHARS    = 200;

    public interface OnRequestSentListener {
        void onResult(boolean success);
    }

    private OnRequestSentListener listener;

    public static SendRequestBottomSheet newInstance(String uid, String name,
                                                     String photoUrl, long mutualCount) {
        SendRequestBottomSheet sheet = new SendRequestBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_UID,     uid);
        args.putString(ARG_NAME,    name);
        args.putString(ARG_PHOTO,   photoUrl);
        args.putLong(ARG_MUTUALS,   mutualCount);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnRequestSentListener(OnRequestSentListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_send_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String recipientUid  = getArguments() != null ? getArguments().getString(ARG_UID)    : "";
        String recipientName = getArguments() != null ? getArguments().getString(ARG_NAME)   : "";
        String photoUrl      = getArguments() != null ? getArguments().getString(ARG_PHOTO)  : "";
        long   mutualCount   = getArguments() != null ? getArguments().getLong(ARG_MUTUALS)  : 0L;

        String myUid = FirebaseAuth.getInstance().getUid();

        CircleImageView photo        = view.findViewById(R.id.recipient_photo);
        TextView        nameText     = view.findViewById(R.id.recipient_name);
        TextView        mutualText   = view.findViewById(R.id.mutual_count_text);
        TextInputEditText noteField  = view.findViewById(R.id.request_note);
        TextView        charCounter  = view.findViewById(R.id.char_counter);
        MaterialButton  sendBtn      = view.findViewById(R.id.send_request_btn);
        MaterialButton  cancelBtn    = view.findViewById(R.id.cancel_btn);

        Glide.with(this).load(photoUrl).placeholder(R.drawable.man)
                .circleCrop().into(photo);
        nameText.setText(recipientName);

        mutualText.setText(mutualCount == 0
                ? "New connection"
                : (mutualCount == 1 ? "1 mutual friend" : mutualCount + " mutual friends"));
        mutualText.setVisibility(View.VISIBLE);

        charCounter.setText("0 / " + MAX_CHARS);

        noteField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                charCounter.setText(len + " / " + MAX_CHARS);
                // Warn when close to limit
                charCounter.setAlpha(len > 180 ? 1.0f : 0.6f);
            }
        });

        cancelBtn.setOnClickListener(v -> dismiss());

        sendBtn.setOnClickListener(v -> {
            sendBtn.setEnabled(false);
            String note = noteField.getText() != null
                    ? noteField.getText().toString().trim() : "";

            ConnectionManager mgr = new ConnectionManager(myUid);
            mgr.sendRequest(recipientUid, note, new ConnectionManager.Callback() {
                @Override
                public void onSuccess() {
                    if (listener != null) listener.onResult(true);
                    dismiss();
                }
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (listener != null) listener.onResult(false);
                    sendBtn.setEnabled(true);
                }
            });
        });
    }
}
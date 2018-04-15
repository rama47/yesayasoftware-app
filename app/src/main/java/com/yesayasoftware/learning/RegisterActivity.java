package com.yesayasoftware.learning;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.basgeekball.awesomevalidation.ValidationStyle;
import com.basgeekball.awesomevalidation.utility.RegexTemplate;
import com.yesayasoftware.learning.entities.AccessToken;
import com.yesayasoftware.learning.entities.ApiError;
import com.yesayasoftware.learning.network.ApiService;
import com.yesayasoftware.learning.network.RetrofitBuilder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    @BindView(R.id.til_name)
    TextInputLayout tilName;
    @BindView(R.id.til_email)
    TextInputLayout tilEmail;
    @BindView(R.id.til_password)
    TextInputLayout tilPassword;

    ApiService service;
    Call<AccessToken> call;
    AwesomeValidation validator;
    TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ButterKnife.bind(this);

        service = RetrofitBuilder.createService(ApiService.class);
        validator = new AwesomeValidation(ValidationStyle.TEXT_INPUT_LAYOUT);
        tokenManager = TokenManager.getInstance(getSharedPreferences("prefs", MODE_PRIVATE));

        setupRules();

    }

    @OnClick(R.id.btn_register)
    void register() {

        String name = tilName.getEditText().getText().toString();
        String email = tilEmail.getEditText().getText().toString();
        String password = tilPassword.getEditText().getText().toString();

        call = service.register(name, email, password);

        call.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                Log.w(TAG, "onResponse: " + response);

                if (response.isSuccessful()) {

                    Log.w(TAG, "onResponse: " + response.body());

                    tokenManager.saveToken(response.body());

                } else {

                    Log.w(TAG, "onFailure: " + response);

                    handleErrors(response.errorBody());

                }

            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                Log.w(TAG, "onFailure: " + t.getMessage());
            }
        });

    }

    private void handleErrors(ResponseBody response) {

        ApiError apiError = Utils.convertErrors(response);

        for (Map.Entry<String, List<String>>  error : apiError.getErrors().entrySet()) {

            if(error.getKey().equals("name")) {

                tilName.setError(error.getValue().get(0));

            }

            if(error.getKey().equals("email")) {

                tilEmail.setError(error.getValue().get(0));

            }

            if(error.getKey().equals("password")) {

                tilPassword.setError(error.getValue().get(0));

            }

        }

    }

    public void setupRules() {

        validator.addValidation(this, R.id.til_name, RegexTemplate.NOT_EMPTY, R.string.err_name);
        validator.addValidation(this, R.id.til_email, Patterns.EMAIL_ADDRESS, R.string.err_email);
        validator.addValidation(this, R.id.til_password, "[a-zA-Z0-9]{6,}", R.string.err_password);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (call != null) {
            call.cancel();
            call = null;
        }

    }

}

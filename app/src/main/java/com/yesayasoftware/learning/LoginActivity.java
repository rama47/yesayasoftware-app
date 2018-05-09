package com.yesayasoftware.learning;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.basgeekball.awesomevalidation.ValidationStyle;
import com.basgeekball.awesomevalidation.utility.RegexTemplate;
import com.yesayasoftware.learning.entities.AccessToken;
import com.yesayasoftware.learning.entities.ApiError;
import com.yesayasoftware.learning.network.ApiService;
import com.yesayasoftware.learning.network.RetrofitBuilder;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    @BindView(R.id.til_email)
    TextInputLayout tilEmail;
    @BindView(R.id.til_password)
    TextInputLayout tilPassword;

    ApiService service;
    Call<AccessToken> call;
    TokenManager tokenManager;
    AwesomeValidation validator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        service = RetrofitBuilder.createService(ApiService.class);
        tokenManager = TokenManager.getInstance(getSharedPreferences("prefs", MODE_PRIVATE));
        validator = new AwesomeValidation(ValidationStyle.TEXT_INPUT_LAYOUT);

        setupRules();

        if (tokenManager.getToken().getAccessToken() != null) {
            startActivity(new Intent(LoginActivity.this, PostActivity.class));
            finish();
        }
    }

    @OnClick(R.id.btn_login)
    void login() {
        String email = tilEmail.getEditText().getText().toString();
        String password = tilPassword.getEditText().getText().toString();

        tilEmail.setError(null);
        tilPassword.setError(null);

        validator.clear();

        if (validator.validate()) {
            call = service.login(email, password);

            call.enqueue(new Callback<AccessToken>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                    if (response.isSuccessful()) {
                        tokenManager.saveToken(response.body());

                        startActivity(new Intent(LoginActivity.this, PostActivity.class));
                        finish();
                    } else {
                        if (response.code() == 422) {
                            handleErrors(response.errorBody());
                        }

                        if (response.code() == 401) {
                            ApiError apiError = Utils.convertErrors(response.errorBody());

                            Toast.makeText(LoginActivity.this, apiError.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    Log.w(TAG, "onFailure: " + t.getMessage());
                }
            });
        }
    }

    @OnClick(R.id.go_to_register)
    void getToRegister() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }

    private void handleErrors(ResponseBody response) {
        ApiError apiError = Utils.convertErrors(response);

        for (Map.Entry<String, List<String>>  error : apiError.getErrors().entrySet()) {
            if(error.getKey().equals("email")) {
                tilEmail.setError(error.getValue().get(0));
            }

            if(error.getKey().equals("password")) {
                tilPassword.setError(error.getValue().get(0));
            }
        }
    }

    public void setupRules() {
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

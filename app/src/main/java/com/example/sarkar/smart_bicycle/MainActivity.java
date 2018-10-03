package com.example.sarkar.smart_bicycle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextInputLayout passwordWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DatabaseHandler db = new DatabaseHandler(getBaseContext());
        TextView name = (TextView)findViewById(R.id.namee);
        int id = db.userExist();
        if(id>-1){
            assert name != null;
            name.setText(db.getUserData(id).getName());
        }
        passwordWrapper = (TextInputLayout) findViewById(R.id.pil);
        passwordWrapper.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    onUnlock(passwordWrapper.getRootView());
                }
                return false;
            }
        });
    }

    public void onRide(View view) {
        Intent in = new Intent(MainActivity.this, RideActivity.class);
        startActivity(in);
    }
    public void onData(View view) {
        Intent in = new Intent(MainActivity.this, ViewDataActivity.class);
        startActivity(in);
    }
    public void onAnalysis(View view) {
        Intent in = new Intent(MainActivity.this, AnalysisActivity.class);
        startActivity(in);
    }
    public void onEdit(View view) {
        Intent in = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(in);
    }

    public void onUnlock(View view) {
        Intent in = new Intent(MainActivity.this, Encryption.class);
        hideKeyboard();
        String password = passwordWrapper.getEditText().getText().toString();
        if(password.isEmpty()){
            passwordWrapper.setError("Password can't be empty");
        }
        else{
            passwordWrapper.setErrorEnabled(false);
            in.putExtra("g",password);
            startActivityForResult(in,1);
        }
    }

    public void onLock(View view){
        Intent in = new Intent(MainActivity.this, Encryption.class);
        in.putExtra("l","l");
        startActivityForResult(in,2);
    }

    public void onChangePass(View view){
        Intent in = new Intent(MainActivity.this, Encryption.class);
        String password = passwordWrapper.getEditText().getText().toString();
        in.putExtra("p",password);
        startActivityForResult(in,3);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Bundle extras = new Bundle();
        if(intent!=null){
            extras = intent.getExtras();
        }
        if(requestCode==1){
            if(extras.containsKey("key") && resultCode == RESULT_OK){
                String val = extras.getString("key");
                if(val != null && val.compareTo("1") == 0){
                    Snackbar.make(this.getWindow().getDecorView(),
                            "Bicycle unlocked!", Snackbar.LENGTH_LONG).show();
                    passwordWrapper.setVisibility(View.GONE);
                    findViewById(R.id.unlock).setVisibility(View.GONE);
                    findViewById(R.id.lock).setVisibility(View.VISIBLE);
                }
                else{
                    Snackbar.make(this.getWindow().getDecorView(),
                            "Wrong password!", Snackbar.LENGTH_LONG).show();
                }
            }
            else if(extras.containsKey("error") && resultCode == RESULT_CANCELED){
                Snackbar.make(this.getWindow().getDecorView(),
                        extras.getString("error","Some error occurred!"), Snackbar.LENGTH_LONG).show();
            }
        }
        if(requestCode==2){
            if(resultCode == RESULT_OK) {
                passwordWrapper.setVisibility(View.VISIBLE);
                findViewById(R.id.unlock).setVisibility(View.VISIBLE);
                findViewById(R.id.lock).setVisibility(View.GONE);
            }
            else if(extras.containsKey("error")) {
                Snackbar.make(this.getWindow().getDecorView(),
                        extras.getString("error","Some error occurred!"), Snackbar.LENGTH_LONG).show();
            }
        }
        if(requestCode==3){
            if(resultCode==RESULT_OK) {
                Snackbar.make(this.getWindow().getDecorView(),
                        "Password changed successfully", Snackbar.LENGTH_SHORT).show();
            }
            else if(extras.containsKey("error")) {
                Snackbar.make(this.getWindow().getDecorView(),
                        extras.getString("error","Some error occurred!"), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /*@Override
    protected void onDestroy(){
        super.onDestroy();
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter!=null) {
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
        }
    }*/

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}

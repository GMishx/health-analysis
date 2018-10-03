package com.example.sarkar.smart_bicycle;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    volatile userData us;
    TextInputLayout name;
    DatabaseHandler db;
    Button dateView;
    Calendar birth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        name = (TextInputLayout) findViewById(R.id.name);
        TextView dist = (TextView) findViewById(R.id.dist);
        dateView = (Button) findViewById(R.id.dob);

        db = new DatabaseHandler(getBaseContext());
        final int id = db.userExist();
        birth = new GregorianCalendar();
        birth.setTime(new Date());
        us = new userData();
        if (id > -1) {
            us = db.getUserData(id);
            name.getEditText().setText(us.getName());
            birth.setTime(new Date(us.getDOB()));
            dist.setText(String.format(Locale.ENGLISH, "%.2f", us.getDist()));
        }
        showDate(birth.get(Calendar.YEAR), birth.get(Calendar.MONTH), birth.get(Calendar.DAY_OF_MONTH));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Save the details?", Snackbar.LENGTH_LONG)
                        .setAction("Save", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                us.setName(name.getEditText().getText().toString());
                                if (id > -1)
                                    db.updateUser(us);
                                else
                                    db.addUser(us);
                            }
                        }).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @SuppressWarnings("deprecation")
    public void setDate(View view) {
        showDialog(999);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 999) {
            DatePickerDialog dd = new DatePickerDialog(this, myDateListener,
                    birth.get(Calendar.YEAR), birth.get(Calendar.MONTH), birth.get(Calendar.DAY_OF_MONTH));
            dd.getDatePicker().setMaxDate((new Date()).getTime());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dd.getDatePicker().setFirstDayOfWeek(Calendar.SUNDAY);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dd.getWindow().setBackgroundDrawable(
                        new ColorDrawable(
                                getBaseContext().getResources().getColor(
                                        android.support.design.R.color.background_material_light, getTheme()
                                )
                        )
                );
            }
            return dd;
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener myDateListener = new
            DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker arg0,
                                      int arg1, int arg2, int arg3) {
                    showDate(arg1, arg2, arg3);
                }
            };

    private void showDate(int year, int month, int day) {
        Calendar upBirth = new GregorianCalendar(year, month, day);
        us.setDOB(upBirth.getTime().getTime());
        dateView.setText(new StringBuilder().append(day).append("/")
                .append(month + 1).append("/").append(year));
    }

}

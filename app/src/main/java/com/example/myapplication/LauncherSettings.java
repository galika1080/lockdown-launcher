package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

public class LauncherSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        getWindow().setStatusBarColor(getColor(R.color.colorAccent));

        androidx.appcompat.widget.Toolbar toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar action = getSupportActionBar();
        action.setDisplayHomeAsUpEnabled(true);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View ret = super.onCreateView(inflater, container, savedInstanceState);

            ret.setBackgroundColor(getContext().getColor(R.color.colorDimBackground));
            return ret;
        }

        void updatePrefAvailability() {
            SharedPreferences settings = getPreferenceManager().getSharedPreferences();

            boolean locked = settings.getBoolean("locked", false);
            String unlockMode = settings.getString("unlock_mode", "Delay");

            Objects.requireNonNull(findPreference("unlock_mode")).setEnabled(!locked);
            Objects.requireNonNull(findPreference("unlock")).setEnabled(locked);
            Objects.requireNonNull(findPreference("lockdown")).setEnabled(!locked);

            Objects.requireNonNull(findPreference("delay_duration")).setEnabled((!locked) && unlockMode.equals("Delay"));
            Objects.requireNonNull(findPreference("password")).setEnabled((!locked) && unlockMode.equals("Password"));
            Objects.requireNonNull(findPreference("num_pages")).setEnabled(!locked);
            Objects.requireNonNull(findPreference("num_cats")).setEnabled(!locked);
            Objects.requireNonNull(findPreference("num_locked")).setEnabled(!locked);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Preference.OnPreferenceChangeListener requestReboot = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ((MainActivity) getActivity()).refreshPager();
                        }
                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // option two
                        }
                    }).setMessage("Reload and apply changes now?");

                    builder.create().show();

                    Log.wtf(preference.getKey(), preference.getKey());

                    return true;
                }
            };

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            updatePrefAvailability();

            final SharedPreferences settings = getPreferenceManager().getSharedPreferences();
            final SharedPreferences.Editor edit = settings.edit();

            Objects.requireNonNull(findPreference("password")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    final EditText input = new EditText(getContext());
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    builder.setView(input);

                    builder.setTitle("Enter new password");

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String newPass = input.getText().toString();

                            settings.edit().putString("pwdHash", getShaString(newPass)).apply();
                            input.setText("");
                            Toast.makeText(getContext(), "Password set", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                    input.requestFocus();
                    input.requestFocusFromTouch();

                    return false;
                }
            });

            Objects.requireNonNull(findPreference("lockdown")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    edit.putBoolean("locked", true).apply();
                    edit.putLong("request_time", -1).apply();
                    edit.putLong("unlock_time", -1).apply();

                    updatePrefAvailability();

                    Toast toast = Toast.makeText(getContext(), "Lockdown successful", Toast.LENGTH_SHORT);
                    toast.show();

                    ((MainActivity) getActivity()).checkLockStatus(null);

                    return false;
                }
            });

            Objects.requireNonNull(findPreference("unlock")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String method = settings.getString("unlock_mode", "defval");
                    String msg = "";

                    if (method.equals("Delay")) {
                        Date d = new Date();
                        long now = d.getTime() / 1000;
                        long delay = settings.getInt("delay_duration", 0) * 60;
                        edit.putLong("unlock_time", now + delay).apply();
                        edit.putLong("request_time", now).apply();

                        msg = "App drawer will be unlocked in " + delay + " seconds";
                        Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT);
                        toast.show();

                        Objects.requireNonNull(findPreference("unlock")).setEnabled(false);
                        Objects.requireNonNull(findPreference("lockdown")).setEnabled(true);
                    } else if (method.equals("Password")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        final EditText input = new EditText(getContext());
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        builder.setView(input);

                        builder.setTitle("Enter password");

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String attempt = input.getText().toString();

                                String hash = settings.getString("pwdHash", "null");
                                String attemptHash = getShaString(attempt);

                                if (hash.equals(attemptHash)) {
                                    settings.edit().putBoolean("locked", false).apply();
                                    Toast.makeText(getContext(), "Unlock successful", Toast.LENGTH_SHORT).show();
                                    updatePrefAvailability();
                                    ((MainActivity) getActivity()).checkLockStatus(null);
                                } else {
                                    Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                        input.requestFocus();
                        input.requestFocusFromTouch();
                    }

                    ((MainActivity) getActivity()).checkLockStatus(null);

                    return false;
                }
            });

            Objects.requireNonNull(findPreference("unlock_mode")).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o instanceof String && o.equals("Delay")) {
                        findPreference("delay_duration").setEnabled(true);
                        findPreference("password").setEnabled(false);
                    } else {
                        findPreference("delay_duration").setEnabled(false);
                        findPreference("password").setEnabled(true);
                    }
                    return true;
                }
            });

            findPreference("wallpaper").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(Intent.ACTION_SET_WALLPAPER);
                    startActivity(i);
                    return false;
                }
            });

            Objects.requireNonNull(findPreference("num_pages")).setOnPreferenceChangeListener(requestReboot);

            Objects.requireNonNull(findPreference("num_cats")).setOnPreferenceChangeListener(requestReboot);

            Objects.requireNonNull(findPreference("num_locked")).setOnPreferenceChangeListener(requestReboot);

            Objects.requireNonNull(findPreference("dark_text")).setOnPreferenceChangeListener(requestReboot);

            Objects.requireNonNull(findPreference("show_add_icons")).setOnPreferenceChangeListener(requestReboot);
        }
    }

    static String getShaString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            return null;
        }
    }
}
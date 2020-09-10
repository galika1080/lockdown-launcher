package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;

public class LayoutPage extends Fragment {

    enum ResponseCodes {
        APP_DRAWER(0),
        SETTINGS(1);

        public final int code;

        private ResponseCodes(int code) {
            this.code = code;
        }
    }

    ConstraintLayout lockedMessage;
    LinearLayout root;
    Resources res;
    SharedPreferences pref;
    SharedPreferences settings;

    int primaryColor;

    int pagerPosition;

    Context context;
    Activity activity;

    List<View> folders = new ArrayList<>();

    boolean folderOpen = false;

    int pageNum;

    public LayoutPage(int position) {
        pageNum = position;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.layout_page, container, false);

        context = getActivity();
        activity = getActivity();

        root = ret.findViewById(R.id.root_lin_layout);
        lockedMessage = ret.findViewById(R.id.LockedMessage);
        res = getResources();
        pref = context.getSharedPreferences("homeLayout", MODE_PRIVATE);
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (settings.getBoolean("dark_text", false)) {
            primaryColor = R.color.colorPrimaryDark;
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            primaryColor = R.color.colorPrimary;
        }

        if (!pref.contains("first_time_setup")) {
            firstTimeSetup();
        }

        buildFolders();

        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeOthers(v);
            }
        });

        return ret;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((MainActivity)activity).checkLockStatus(this);
    }

    void setPosition(int pos) {
        pagerPosition = pos;
    }

    int getPosition() {
        return pagerPosition;
    }

    void placeLock() {
        lockedMessage.setVisibility(View.VISIBLE);
    }

    void removeLock() {
        lockedMessage.setVisibility(View.INVISIBLE);
    }

    private void firstTimeSetup() {
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean("first_time_setup", true);

        edit.putString("name_cat_1_page_" + pageNum, "Info");
        edit.putString("name_cat_2_page_" + pageNum, "Comms");
        edit.putString("name_cat_3_page_" + pageNum, "Records");
        edit.putString("name_cat_4_page_" + pageNum, "Utils");

        edit.apply();

        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("pwdHash", LauncherSettings.getShaString("")).apply();
    }

    public void closeOthers(View v) {
        folderOpen = false;

        for (View f : folders) {
            if (f != v) {
                //f.setVisibility(View.INVISIBLE);
                fadeOut(f);
                TextView label = getLabelForFolder((LinearLayout) f);

                if (label == null) continue;

                //label.setVisibility(View.VISIBLE);
                fadeIn(label);
            }
        }
    }

    private void buildFolders() {
        int numFolders = Integer.parseInt(settings.getString("num_cats", "4"));

        Drawable horiz_line = res.getDrawable(R.drawable.horiz_line, activity.getTheme());

        for (int i = 0; i < numFolders; i++) {
            String nameKey = "name_cat_" + (i + 1) + "_page_" + pageNum;
            String name = pref.getString(nameKey, "[long press]");

            FrameLayout frame = new FrameLayout(context);              // make the frame
            frame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView label = new TextView(context);
            label.setTag(nameKey);
            label.setText(name);
            label.setTextAppearance(R.style.TextAppearance_AppCompat_Display3);
            label.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            label.setOnClickListener(folderOnClick(label));
            label.setOnLongClickListener(folderOnHold(label));
            label.setOnEditorActionListener(folderOnSubmit(label));
            label.setTextColor(activity.getColor(primaryColor));

            LinearLayout folder = new LinearLayout(context);
            folder.setOrientation(LinearLayout.HORIZONTAL);
            folder.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            folder.setVisibility(View.INVISIBLE);
            folders.add(folder);

            frame.addView(label);
            frame.addView(folder);
            root.addView(frame);

            placeIcons(folder);

            if (i != numFolders - 1) { // potentially add a divider
                final ImageView divider = new ImageView(context);
                divider.setImageDrawable(horiz_line);
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                divider.setScaleType(ImageView.ScaleType.FIT_XY);
                divider.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            closeOthers(divider);
                        }
                    });
                divider.setColorFilter(activity.getColor(primaryColor), PorterDuff.Mode.SRC_ATOP);

                root.addView(divider);
            }
        }

        Space space = new Space(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 30));
        root.addView(space);
    }

    private View.OnClickListener folderOnClick(View v) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout folder = getFolderForLabel((TextView) v);
                if (folder == null) return;

                boolean wasOpen = folderOpen;
                if (!folderOpen) {
                    //v.setVisibility(View.INVISIBLE);
                    fadeOut(v);
                    //folder.setVisibility(View.VISIBLE);
                    fadeIn(folder);
                }

                closeOthers(folder);

                if (!wasOpen) folderOpen = true;
            }
        };
    }

    private TextView.OnEditorActionListener folderOnSubmit(TextView text) {
        return new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView text, int action, KeyEvent event) {
                if (action == EditorInfo.IME_ACTION_DONE) {
                    text.clearFocus();
                    text.setOnClickListener(folderOnClick(text));

                    text.setFocusableInTouchMode(false);
                    text.setInputType(InputType.TYPE_NULL);

                    SharedPreferences.Editor edit = pref.edit();
                    edit.putString((String) text.getTag(), text.getText().toString());
                    edit.apply();
                }
                return false;
            }
        };
    }

    private View.OnLongClickListener folderOnHold(View v) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TextView text = (TextView) v;
                text.setOnClickListener(null);

                text.setFocusableInTouchMode(true);
                text.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                return false;
            }
        };
    }

    private LinearLayout getFolderForLabel(TextView v) {
        ViewGroup group = (ViewGroup) v.getParent();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof LinearLayout) {
                return (LinearLayout) child;
            }
        }

        return null;
    }

    private TextView getLabelForFolder(LinearLayout v) {
        ViewGroup group = (ViewGroup) v.getParent();
        if (group == null) {
            return null;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            }
        }

        return null;
    }

    public void requestUnlock(View v) {
        ((MainActivity) getActivity()).pager.setCurrentItem(0);
    }

    private void placeIcons(LinearLayout v) {
        for (int i = 0; i < 4; i++) {
            String folderNumber = (String) (getLabelForFolder(v).getTag());

            Pattern p = Pattern.compile("name_cat_(\\d+)_page_\\d+");
            Matcher m = p.matcher(folderNumber);
            if (!m.find()) {
                continue;
            }
            folderNumber = m.group(1);

            String keyName = "pkg_cat_" + folderNumber + "_" + (i + 1) + "_page_" + pageNum;
            String packageName = pref.getString(keyName, null);

            boolean showAddIcons = settings.getBoolean("show_add_icons", true);

            Drawable icon = showAddIcons ? res.getDrawable(R.drawable.add_circle, activity.getTheme()) : null;
            if (packageName != null) {
                icon = getActivityIcon(packageName);
            }

            ImageView button = new ImageView(context);
            button.setTag(keyName);
            button.setImageDrawable(icon);
            button.setOnClickListener(iconOnClick(button));
            button.setOnLongClickListener(iconOnHold(button));
            button.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            button.setScaleType(ImageView.ScaleType.FIT_CENTER);
            v.addView(button);
        }
    }

    private View.OnClickListener iconOnClick(View v) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(v instanceof ImageView)) return;

                String pkg = pref.getString((String) v.getTag(), null);

                String result = ((MainActivity) activity).iconTapped(pkg);

                if (result != null) { // if main activity tells us we need to place an icon, do so
                    setIconPackage((ImageView) v, result);
                } else if (pkg == null) { // if there's no package set for this icon, open drawer
                    v.performLongClick();
                } else { // launch this icon's assigned package
                    closeOthers(null);
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                    startActivity(launchIntent);
                }
            }
        };
    }

    private ImageView workingIcon = null;

    private View.OnLongClickListener iconOnHold(View v) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast toast = Toast.makeText(context, "Choose an app to place or launch", Toast.LENGTH_SHORT);
                toast.show();

                ((MainActivity) activity).setPreviousPage();
                ((MainActivity) activity).showAppDrawer();

                return true;
            }
        };
    }

    private void setIconPackage(ImageView v, String packageName) {
        SharedPreferences.Editor edit = pref.edit();
        edit.putString((String) v.getTag(), packageName);
        edit.apply();

        Drawable icon = res.getDrawable(R.drawable.add_circle, activity.getTheme());
        if (packageName != null) {
            icon = getActivityIcon(packageName);
        }
        v.setImageDrawable(icon);
    }

    private Drawable getActivityIcon(String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void fadeIn(View v) {
        //v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);

        v.animate()
            .alpha(1f)
            .setDuration(180)
            .setListener(null);
    }

    private void fadeOut(final View v) {
        v.setAlpha(1f);

        v.animate()
            .alpha(0f)
            .setDuration(180)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator anim) {
                    v.setVisibility(View.INVISIBLE);
                }
            });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ResponseCodes.APP_DRAWER.code && data != null) {
            String packageName = data.getStringExtra("package");

            SharedPreferences.Editor edit = pref.edit();
            edit.putString((String) workingIcon.getTag(), packageName);
            edit.apply();

            Drawable icon = res.getDrawable(R.drawable.add_circle, activity.getTheme());
            if (packageName != null) {
                icon = getActivityIcon(packageName);
            }
            workingIcon.setImageDrawable(icon);
        }

        if (requestCode == ResponseCodes.SETTINGS.code) {
            Intent intent = activity.getIntent();
            activity.finish();
            startActivity(intent);
        }
    }

    public void launchClock(View v) {
        Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
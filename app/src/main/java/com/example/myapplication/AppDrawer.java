package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

public class AppDrawer extends Fragment {

    GridView root;
    FrameLayout frame;
    String keyName;
    Context context;
    FragmentActivity activity;

    ConstraintLayout lockedMessage;

    FrameLayout popupFrame = null;

    int pagerPosition;

    CustomAdapter adapter;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        adapter = new CustomAdapter(context);
        new AppDrawerProvider(this, adapter).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.app_drawer, container, false);

        context = getActivity();
        activity = getActivity();

        root = ret.findViewById(R.id.gridview);
        frame = ret.findViewById(R.id.appdraw_frame);
        lockedMessage = ret.findViewById(R.id.LockedMessageDrawer);

        root.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (popupFrame != null) {
                    fadeOut(popupFrame);
                }
            }
        });

        root.setAdapter(adapter);

        return ret;
    }

    void placeLock() {
        lockedMessage.setVisibility(View.VISIBLE);
    }

    void removeLock() {
        lockedMessage.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((MainActivity)activity).checkLockStatus(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        ((MainActivity)activity).checkLockStatus(this);
    }

    void setPosition(int pos) {
        pagerPosition = pos;
    }

    int getPosition() {
        return pagerPosition;
    }

    View.OnClickListener iconOnClick(View v) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupFrame != null) {
                    fadeOut(popupFrame);
                    return;
                }

                int[] loc = new int[2];
                v.getLocationOnScreen(loc);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(loc[0], loc[1], 0, 0);
                FrameLayout popup = new FrameLayout(context);
                popup.setLayoutParams(lp);

                LinearLayout options = new LinearLayout(context);
                options.setOrientation(LinearLayout.VERTICAL);

                LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                ImageView panel = new ImageView(context);
                panel.setImageDrawable(context.getDrawable(R.drawable.ic_roundsquare));
                panel.setLayoutParams(lp1);
                panel.setScaleType(ImageView.ScaleType.FIT_XY);
                popup.addView(panel);

                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp2.setMargins(20, 20, 20, 20);
                TextView submit = new TextView(context);
                submit.setText("Place");
                submit.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                submit.setTextColor(context.getColor(R.color.colorPrimaryDark));
                submit.setOnClickListener(selectOnClick(submit));
                submit.setTag(v.getTag());
                submit.setLayoutParams(lp2);

                TextView launch = new TextView(context);
                launch.setText("Launch");
                launch.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
                launch.setTextColor(context.getColor(R.color.colorPrimaryDark));
                launch.setOnClickListener(launchOnClick(launch));
                launch.setTag(v.getTag());
                launch.setLayoutParams(lp2);

                options.addView(launch);
                options.addView(submit);

                popup.addView(options);
                popup.setScaleX(0f);
                popup.setScaleY(0f);
                frame.addView(popup);
                popupFrame = popup;
                fadeIn(popupFrame);
            }
        };
    }

    private View.OnClickListener selectOnClick(View v) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).beginAppPlacement((String) v.getTag());
                ((MainActivity) activity).restorePreviousPage();

                fadeOut(popupFrame);
            }
        };
    }

    private View.OnClickListener launchOnClick(View v) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage((String) v.getTag());
                startActivity(launchIntent);

                fadeOut(popupFrame);
            }
        };
    }

    private void fadeIn(View v) {
        v.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .setListener(null);
    }

    private void fadeOut(final View v) {
        //v.setAlpha(1f);

        v.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator anim) {
                        frame.removeView(v);
                        popupFrame = null;
                    }
                });
    }

    public class CustomAdapter extends BaseAdapter {

        List<Pair<String, Drawable>> apps = new ArrayList<>();

        public CustomAdapter (Context c) {
        }

        public int getCount() {
            return apps.size();
        }

        public Pair<String, Drawable> getItem(int position) {
            return apps.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            /*if (convertView instanceof ImageView) {
                ((ImageView) convertView).setImageDrawable(apps.get(position).second);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 200, 1);
                params.setMargins(0, 20, 0, 20);
                convertView.setLayoutParams(params);
                ((ImageView) convertView).setScaleType(ImageView.ScaleType.FIT_CENTER);
                convertView.setOnClickListener(iconOnClick(convertView));
                convertView.setTag(apps.get(position).first);

                return convertView;
            }*/

            ImageView icon = new ImageView(context);
            icon.setImageDrawable(apps.get(position).second);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150, 1);
            icon.setLayoutParams(params);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setOnClickListener(iconOnClick(icon));
            icon.setTag(apps.get(position).first);

            return icon;
        }
    }

    public void requestUnlock(View v) {
        ((MainActivity) getActivity()).pager.setCurrentItem(0);
    }
}
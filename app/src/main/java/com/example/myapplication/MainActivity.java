package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ViewPager2 pager;
    Context context;

    Runnable updater;

    SharedPreferences settings;

    String placingPackage = null;
    int previousPage = -1;

    void checkLockStatus(final Fragment extraFragment) {
        boolean locked = settings.getBoolean("locked", false);
        if (locked) {
            final ProgressBar bar = findViewById(R.id.unlock_progress);
            final Handler timerHandler = new Handler();

            updater = new Runnable() {
                @Override
                public void run() {
                    Date d = new Date();
                    long start = settings.getLong("request_time", -1);

                    if (start == -1) {
                        if (settings.getBoolean("locked", false)) {
                            bar.setProgress(0, true);
                        }
                        return;
                    }

                    long end = settings.getLong("unlock_time", 0);
                    long now = d.getTime() / 1000;
                    double elapsed = now - start;
                    double total = end - start;
                    double prog = 1000 * elapsed / total;

                    if (prog < 1000) {
                        bar.setProgress((int) prog, true);
                        timerHandler.postDelayed(updater, 1000);

                        lock(extraFragment);
                    } else {
                        settings.edit().putBoolean("locked", false).apply();
                        settings.edit().putLong("request_time", -1).apply();
                        bar.setProgress(1000, true);

                        unlock(extraFragment);
                    }
                }
            };
            timerHandler.post(updater);

            lock(extraFragment);
        } else {
            final ProgressBar bar = findViewById(R.id.unlock_progress);
            bar.setProgress(1000, false);

            unlock(extraFragment);
        }
    }

    void showAppDrawer() {
        pager.setCurrentItem(pager.getAdapter().getItemCount() - 1);
    }

    void unlock(Fragment ef) {
        if (pager.getAdapter() == null) return;

        List<Fragment> frags = new ArrayList<>(((PageAdapter) pager.getAdapter()).getFragments());

        if (ef != null) frags.add(ef);

        for (Fragment f : frags) {
            if (f.getView() == null) continue; // has not set up layout yet

            if (f instanceof LayoutPage) {
                ((LayoutPage) f).removeLock();
            }

            if (f instanceof AppDrawer) {
                ((AppDrawer) f).removeLock();
            }

            if (f instanceof LauncherSettings.SettingsFragment) {
                ((LauncherSettings.SettingsFragment) f).updatePrefAvailability();
            }
        }
    }

    void lock(Fragment ef) {
        if (pager.getAdapter() == null) return;

        List<Fragment> frags = new ArrayList<>(((PageAdapter) pager.getAdapter()).getFragments());

        int numLocked = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("num_locked", "1"));
        int numPages = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("num_pages", "1"));

        if (ef != null) frags.add(ef);

        for (Fragment f : frags) {
            if (f.getView() == null) continue; // has not set up layout yet

            if (f instanceof LayoutPage) {
                if (((LayoutPage) f).getPosition() < Math.max(numPages - numLocked + 1, 1)) continue;
                ((LayoutPage) f).placeLock();
            }

            if (f instanceof AppDrawer) {
                if (((AppDrawer) f).getPosition() < Math.max(numPages - numLocked + 1, 1)) continue;
                ((AppDrawer) f).placeLock();
            }
        }
    }

    void beginAppPlacement(String pkg) {
        // store pkg in an instance variable
        placingPackage = pkg;

        // display message about placing whatever app
        try {
            ((TextView) findViewById(R.id.placingAppName)).setText("Pick a slot for " + getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        findViewById(R.id.placingAppMessage).setVisibility(View.VISIBLE);
    }

    public void cancelAppPlacement(View v) {
        placingPackage = null;
        previousPage = -1;
        findViewById(R.id.placingAppMessage).setVisibility(View.GONE);
    }

    String iconTapped(String pkg) {
        if (placingPackage == null) {
            return null;
        } else {
            String ret = placingPackage;
            placingPackage = null;
            previousPage = -1;
            findViewById(R.id.placingAppMessage).setVisibility(View.GONE);

            return ret;
        }
    }

    void setPreviousPage() {
        previousPage = pager.getCurrentItem();
    }

    void restorePreviousPage() {
        if (previousPage != -1) {
            pager.setCurrentItem(previousPage);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        context = this;

        pager = findViewById(R.id.pager);
        PageAdapter adapter = new PageAdapter(this);
        pager.setAdapter(adapter);
        pager.setCurrentItem(1);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    void refreshPager() {
        PageAdapter adapter = new PageAdapter(this);
        pager.setAdapter(adapter);
    }

    public void goToSettings(View v) {
        pager.setCurrentItem(0);
    }

    private class PageAdapter extends FragmentStateAdapter {
        int numPages;
        private List<Fragment> fragments = new ArrayList<>();

        public PageAdapter(FragmentActivity fa) {
            super(fa);
            numPages = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("num_pages", "1")) + 2;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment ret;

            if (position == 0) {
                ret = new LauncherSettings.SettingsFragment();
            } else if (position == numPages - 1) {
                ret = new AppDrawer();
                ((AppDrawer) ret).setPosition(position);
            } else {
                ret = new LayoutPage(position - 1);
                ((LayoutPage) ret).setPosition(position);
            }

            fragments.add(ret);

            return ret;
        }

        List<Fragment> getFragments() {
            return fragments;
        }

        @Override
        public int getItemCount() {
            return numPages;
        }
    }
}
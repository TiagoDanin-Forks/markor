/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsMenuItemDummy;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A common base fragment to extend from
 */
@SuppressWarnings("unused")
public abstract class GsFragmentBase<AS extends GsSharedPreferencesPropertyBackend, CU extends GsContextUtils> extends Fragment {

    private boolean _fragmentFirstTimeVisible = true;
    protected AS _appSettings;
    protected CU _cu;
    protected Bundle _savedInstanceState = null;
    protected Menu _fragmentMenu = new GsMenuItemDummy.Menu();
    protected Queue<Runnable> _postTasks = new LinkedList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Inflate the fragments layout. Don't override this method, just supply the needed
     * {@link LayoutRes} via abstract method {@link #getLayoutResId()}, super does the rest
     */
    @Deprecated
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final Activity activity = getActivity();
        _cu = createContextUtilsInstance(activity);
        _appSettings = createAppSettingsInstance(activity);

        _cu.setAppLanguage(activity, getAppLanguage());
        _savedInstanceState = savedInstanceState;

        if (getLayoutResId() == 0) {
            Log.e(getClass().getCanonicalName(), "Error: GsFragmentbase.onCreateview: Returned 0 for getLayoutResId");
        }

        return inflater.inflate(getLayoutResId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.postDelayed(this::checkRunFirstTimeVisible, 200);
    }

    protected abstract AS createAppSettingsInstance(final Context context);

    protected abstract CU createContextUtilsInstance(final Context context);

    /**
     * Get a tag from the fragment, allows faster distinction
     *
     * @return This fragments tag
     */
    public abstract String getFragmentTag();


    /**
     * Get the layout to be inflated in the fragment
     *
     * @return Layout resource id
     */
    @LayoutRes
    protected abstract int getLayoutResId();

    /**
     * Event to be called when the back button was pressed
     * True should be returned when this was handled by the fragment
     * and  no further handling in the view hierarchy is needed
     *
     * @return True if back handled by fragment
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * Set the language to be used in this fragment
     * Defaults to resolve the language from sharedpreferences: pref_key__language
     *
     * @return Empty string for system language, or an android locale code
     */
    public String getAppLanguage() {
        if (getContext() != null) {
            return getContext().getSharedPreferences("app", Context.MODE_PRIVATE)
                    .getString("pref_key__language", "");
        }
        return "";
    }

    /**
     * This will be called when this fragment gets the first time visible
     */
    protected void onFragmentFirstTimeVisible() {
    }

    private void checkRunFirstTimeVisible() {
        if (_fragmentFirstTimeVisible && isVisible() && isResumed()) {
            _fragmentFirstTimeVisible = false;
            onFragmentFirstTimeVisible();
        }
    }

    public void post(final Runnable action) {
        final View view = getView();
        if (isResumed() && view != null) {
            view.post(action);
        } else {
            _postTasks.add(action);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final View view = getView();
        if (view != null) {
            view.post(this::checkRunFirstTimeVisible);
            // Add any remaining tasks
            while (!_postTasks.isEmpty()) {
                view.post(_postTasks.remove());
            }
        }
        _postTasks.clear();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        _fragmentMenu = menu;
    }

    public Menu getFragmentMenu() {
        return _fragmentMenu;
    }


    public boolean onReceiveKeyPress(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Called when on @{{@link Toolbar#setOnClickListener(View.OnClickListener)}} click
     *
     * @param v Toolbar @{{@link View}}
     */
    protected void onToolbarClicked(final View v) {
    }

    /**
     * Called when on @{{@link Toolbar#setOnLongClickListener(View.OnLongClickListener)}} long click
     *
     * @param v Toolbar @{{@link View}}
     * @return Toolbar long click was handled = true
     */
    protected boolean onToolbarLongClicked(final View v) {
        return false;
    }
}

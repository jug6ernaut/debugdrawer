package com.jakewharton.u2020.ui.bugreport;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.Toast;
import com.jakewharton.u2020.util.Intents;
import com.jug6ernaut.debugdrawer.BuildConfig;
import com.jug6ernaut.debugdrawer.utils.Strings;
import com.mattprecious.telescope.Lens;
import io.jug6ernaut.debugdrawer.modules.data.LumberYard;

import java.io.File;
import java.util.ArrayList;

import static com.jakewharton.u2020.ui.bugreport.BugReportDialog.ReportListener;
import static com.jakewharton.u2020.ui.bugreport.BugReportView.Report;

/**
 * Pops a dialog asking for more information about the bug report and then creates an email with a
 * JIRA-formatted body.
 */
public final class BugReportLens extends Lens implements ReportListener  {
	private final Context context;
	private final LumberYard lumberYard;

	private File screenshot;

	public BugReportLens(Context context, LumberYard lumberYard) {
		this.context = context;
		this.lumberYard = lumberYard;
	}

	@Override public void onCapture(File screenshot) {
		this.screenshot = screenshot;

		com.jakewharton.u2020.ui.bugreport.BugReportDialog dialog = new com.jakewharton.u2020.ui.bugreport.BugReportDialog(context);
		dialog.setReportListener(this);
		dialog.show();
	}

	@Override public void onBugReportSubmit(final Report report) {
		if (report.includeLogs) {
			lumberYard.save(new LumberYard.OnSaveLogListener() {
				@Override
				public void onSave(File logs) {
					submitReport(report, logs);
				}
				@Override
				public void onError(String message) {
					Toast.makeText(context, "Couldn't attach the logs.", Toast.LENGTH_SHORT).show();
					submitReport(report, null);
				}
			});
		} else {
			submitReport(report, null);
		}
	}

	private void submitReport(Report report, File logs) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		String densityBucket = getDensityString(dm);

		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.setType("message/rfc822");
		// TODO: intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "u2020-bugs@blackhole.io" });
		intent.putExtra(Intent.EXTRA_SUBJECT, report.title);

		StringBuilder body = new StringBuilder();
		if (!Strings.isBlank(report.description)) {
			body.append("{panel:title=Description}\n").append(report.description).append("\n{panel}\n\n");
		}

		// TODO: Change to dyanimic BuildConfig
		body.append("{panel:title=App}\n");
		body.append("Version: ").append(BuildConfig.VERSION_NAME).append('\n');
		body.append("Version code: ").append(BuildConfig.VERSION_CODE).append('\n');
		body.append("{panel}\n\n");

		body.append("{panel:title=Device}\n");
		body.append("Make: ").append(Build.MANUFACTURER).append('\n');
		body.append("Model: ").append(Build.MODEL).append('\n');
		body.append("Resolution: ")
				.append(dm.heightPixels)
				.append("x")
				.append(dm.widthPixels)
				.append('\n');
		body.append("Density: ")
				.append(dm.densityDpi)
				.append("dpi (")
				.append(densityBucket)
				.append(")\n");
		body.append("Release: ").append(Build.VERSION.RELEASE).append('\n');
		body.append("API: ").append(Build.VERSION.SDK_INT).append('\n');
		body.append("{panel}");

		intent.putExtra(Intent.EXTRA_TEXT, body.toString());

		ArrayList<Uri> attachments = new ArrayList<>();
		if (screenshot != null && report.includeScreenshot) {
			attachments.add(Uri.fromFile(screenshot));
		}
		if (logs != null) {
			attachments.add(Uri.fromFile(logs));
		}

		if (!attachments.isEmpty()) {
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
		}

		Intents.maybeStartActivity(context, intent);
	}

	private static String getDensityString(DisplayMetrics displayMetrics) {
		switch (displayMetrics.densityDpi) {
			case DisplayMetrics.DENSITY_LOW:
				return "ldpi";
			case DisplayMetrics.DENSITY_MEDIUM:
				return "mdpi";
			case DisplayMetrics.DENSITY_HIGH:
				return "hdpi";
			case DisplayMetrics.DENSITY_XHIGH:
				return "xhdpi";
			case DisplayMetrics.DENSITY_XXHIGH:
				return "xxhdpi";
			case DisplayMetrics.DENSITY_XXXHIGH:
				return "xxxhdpi";
			case DisplayMetrics.DENSITY_TV:
				return "tvdpi";
			default:
				return String.valueOf(displayMetrics.densityDpi);
		}
	}
}

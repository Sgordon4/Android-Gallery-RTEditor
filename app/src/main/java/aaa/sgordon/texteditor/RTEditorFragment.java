package aaa.sgordon.texteditor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Bundle;
import android.text.Layout;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.onegravity.rteditor.RTEditText;
import com.onegravity.rteditor.RTManager;
import com.onegravity.rteditor.api.RTApi;
import com.onegravity.rteditor.api.RTMediaFactoryImpl;
import com.onegravity.rteditor.api.RTProxyImpl;
import com.onegravity.rteditor.api.format.RTFormat;
import com.onegravity.rteditor.effects.Effect;
import com.onegravity.rteditor.effects.Effects;

import java.util.List;
import java.util.Set;

import aaa.sgordon.texteditor.databinding.TextRichBinding;

public class RTEditorFragment extends Fragment {
	TextRichBinding binding;

	private RTManager rtManager;
	private RTEditText rtEditText;
	private int textSizePx;

	private MaterialToolbar toolbarTop;
	private LinearLayout toolbarBottom1;
	private LinearLayout toolbarBottom2;


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = TextRichBinding.inflate(inflater, container, false);

		RTApi rtApi = new RTApi(requireContext(), new RTProxyImpl(requireActivity()), new RTMediaFactoryImpl(requireContext(), true));
		rtManager = new RTManager(rtApi, savedInstanceState);


		//Create the editor with the theme it requires
		Context themedContext = new ContextThemeWrapper(requireContext(), com.onegravity.rteditor.R.style.RTE_BaseThemeLight);
		rtEditText = new ObservableRTEditText(themedContext);

		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		rtEditText.setLayoutParams(params);
		rtEditText.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		rtEditText.setHint("Write something...");

		//Get the text color from the theme
		TypedValue typedValue = new TypedValue();
		requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		try (TypedArray arr = requireContext().obtainStyledAttributes(typedValue.data, new int[] { android.R.attr.textColorPrimary })){
			int textColor = arr.getColor(0, Color.BLACK);
			rtEditText.setTextColor(textColor);
		}

		//Add the editor to the ScrollView
		binding.scrollContainer.addView(rtEditText, 0);


		//Register editor & set text
		rtManager.registerEditor(rtEditText, true);

		//String lorem = getResources().getString(R.string.lorem_ipsum_html);
		String lorem = getResources().getString(R.string.lorem_ipsum_small_html);
		rtEditText.setRichTextEditing(true, lorem);

		textSizePx = (int) rtEditText.getTextSize();



		toolbarTop = binding.richToolbarTop;
		toolbarBottom1 = binding.richToolbarBottom1;
		toolbarBottom2 = binding.richToolbarBottom2;


		binding.scrollContainer.post(() -> {
			//Once the ScrollView is laid out, add space to the scroll content so it can scroll past its last line
			int parentHeight = binding.scrollContainer.getHeight();
			int lineHeight = rtEditText.getLineHeight();
			int desiredPadding = Math.max(parentHeight - lineHeight, 0);

			rtEditText.setPadding(rtEditText.getPaddingLeft(), rtEditText.getPaddingTop(), rtEditText.getPaddingRight(), desiredPadding);
		});



		//Keep the bottom toolbars visually attached to the soft keyboard as it expands
		ViewCompat.setWindowInsetsAnimationCallback(toolbarBottom1, new WindowInsetsAnimationCompat
				.Callback(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
			@NonNull
			@Override
			public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets, @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
				Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).toPlatformInsets();
				Insets sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
				int keyboardHeight = Math.max(imeInsets.bottom - sysBarInsets.bottom, 0);

				FrameLayout.LayoutParams toolParams1 = (FrameLayout.LayoutParams) toolbarBottom1.getLayoutParams();
				toolParams1.bottomMargin = keyboardHeight;
				toolbarBottom1.setLayoutParams(toolParams1);

				FrameLayout.LayoutParams toolParams2 = (FrameLayout.LayoutParams) toolbarBottom2.getLayoutParams();
				toolParams2.bottomMargin = keyboardHeight;
				toolbarBottom2.setLayoutParams(toolParams2);

				return insets;
			}
		});


		setupToolbars();

		return binding.getRoot();
	}


	private void printContents() {
		System.out.println(rtEditText.getText(RTFormat.HTML));
	}



	private final Set<Effect<?,?>> excludedFromClear = Set.of(Effects.BULLET, Effects.NUMBER, Effects.ALIGNMENT);
	private void setupToolbars() {

		((ObservableRTEditText) rtEditText).setOnSelectionChangedListener((selStart, selEnd) -> {
			updateActiveButtons();
		});



		toolbarTop.setOnMenuItemClickListener(item -> {
			if(item.getItemId() == R.id.action_undo) rtManager.onUndo();
			else if(item.getItemId() == R.id.action_redo) rtManager.onRedo();

			else if(item.getItemId() == R.id.action_bullet) {
				List<Boolean> styles = Effects.BULLET.valuesInSelection(rtEditText);
				boolean isBulleted = !styles.isEmpty() && styles.get(0);

				if(isBulleted) Effects.BULLET.clearFormattingInSelection(rtEditText);
				else rtManager.onEffectSelected(Effects.BULLET, true);
			}
			else if(item.getItemId() == R.id.action_number) {
				List<Boolean> styles = Effects.NUMBER.valuesInSelection(rtEditText);
				boolean isNumbered = !styles.isEmpty() && styles.get(0);

				if(isNumbered) Effects.NUMBER.clearFormattingInSelection(rtEditText);
				else rtManager.onEffectSelected(Effects.NUMBER, true);
			}

			return true;
		});





		binding.actionClear.setOnClickListener(v -> {
			int selectionSize = rtEditText.getSelectionEnd() - rtEditText.getSelectionStart();
			if(selectionSize == 0) return;

			//Clear all formatting (except excluded formats)
			for(Effect<?, ?> effect : Effects.FORMATTING_EFFECTS) {
				if(excludedFromClear.contains(effect)) continue;
				effect.clearFormattingInSelection(rtEditText);
			}
			updateActiveButtons();
		});


		binding.actionHeading1.setOnClickListener(v -> {
			List<Integer> fontSizes = Effects.FONTSIZE.valuesInSelection(rtEditText);
			boolean isH1 = !fontSizes.isEmpty() && fontSizes.get(0) == (textSizePx*2);

			int selectionSize = rtEditText.getSelectionEnd() - rtEditText.getSelectionStart();
			if(isH1 && selectionSize != 0) Effects.FONTSIZE.clearFormattingInSelection(rtEditText);
			else rtManager.onEffectSelected(Effects.FONTSIZE, textSizePx*2);
			updateActiveButtons();
		});
		binding.actionHeading2.setOnClickListener(v -> {
			List<Integer> fontSizes = Effects.FONTSIZE.valuesInSelection(rtEditText);
			boolean isH2 = !fontSizes.isEmpty() && fontSizes.get(0) == (int)(textSizePx*1.5);

			int selectionSize = rtEditText.getSelectionEnd() - rtEditText.getSelectionStart();
			if(isH2 && selectionSize != 0) Effects.FONTSIZE.clearFormattingInSelection(rtEditText);
			else rtManager.onEffectSelected(Effects.FONTSIZE, (int)(textSizePx*1.5));
			updateActiveButtons();
		});


		binding.actionBold.setOnClickListener(v -> {
			boolean isBold = Effects.BOLD.existsInSelection(rtEditText);
			rtManager.onEffectSelected(Effects.BOLD, !isBold);
			binding.actionBold.setSelected(!isBold);
		});
		binding.actionItalic.setOnClickListener(v -> {
			boolean isItalic = Effects.ITALIC.existsInSelection(rtEditText);
			rtManager.onEffectSelected(Effects.ITALIC, !isItalic);
			binding.actionItalic.setSelected(!isItalic);
		});
		binding.actionUnderline.setOnClickListener(v -> {
			boolean isUnderline = Effects.UNDERLINE.existsInSelection(rtEditText);
			rtManager.onEffectSelected(Effects.UNDERLINE, !isUnderline);
			binding.actionUnderline.setSelected(!isUnderline);
		});
		binding.actionStrikethrough.setOnClickListener(v -> {
			boolean isStrikethrough = Effects.STRIKETHROUGH.existsInSelection(rtEditText);
			rtManager.onEffectSelected(Effects.STRIKETHROUGH, !isStrikethrough);
			binding.actionStrikethrough.setSelected(!isStrikethrough);
		});


		binding.toolbarSwap1.setOnClickListener(v -> toolbarBottom2.setVisibility(View.VISIBLE));


		//----------------------------------------


		//WARNING: The editor does not support RTL, and defaults to left. These buttons still work though.
		binding.actionAlignLeft.setOnClickListener(v -> {
			List<Layout.Alignment> styles = Effects.ALIGNMENT.valuesInSelection(rtEditText);
			boolean isLeft = styles.isEmpty() || styles.get(0) == Layout.Alignment.ALIGN_NORMAL;

			if(isLeft) Effects.ALIGNMENT.clearFormattingInSelection(rtEditText);
			else rtManager.onEffectSelected(Effects.ALIGNMENT, Layout.Alignment.ALIGN_NORMAL);
			updateActiveButtons();
		});
		binding.actionAlignCenter.setOnClickListener(v -> {
			List<Layout.Alignment> styles = Effects.ALIGNMENT.valuesInSelection(rtEditText);
			boolean isCenter = !styles.isEmpty() && styles.get(0) == Layout.Alignment.ALIGN_CENTER;

			if(isCenter) Effects.ALIGNMENT.clearFormattingInSelection(rtEditText);
			else rtManager.onEffectSelected(Effects.ALIGNMENT, Layout.Alignment.ALIGN_CENTER);
			updateActiveButtons();
		});
		binding.actionAlignRight.setOnClickListener(v -> {
			List<Layout.Alignment> styles = Effects.ALIGNMENT.valuesInSelection(rtEditText);
			boolean isRight = !styles.isEmpty() && styles.get(0) == Layout.Alignment.ALIGN_OPPOSITE;

			if(isRight) Effects.ALIGNMENT.clearFormattingInSelection(rtEditText);
			else rtManager.onEffectSelected(Effects.ALIGNMENT, Layout.Alignment.ALIGN_OPPOSITE);
			updateActiveButtons();
		});



		binding.actionFontIncrease.setOnClickListener(v -> {
			List<Integer> fontSizes = Effects.FONTSIZE.valuesInSelection(rtEditText);
			int currSize = fontSizes.isEmpty() ? textSizePx : fontSizes.get(0);
			int nextSize = currSize < textSizePx*2 ? currSize+1 : currSize;

			int selectionSize = rtEditText.getSelectionEnd() - rtEditText.getSelectionStart();
			if(nextSize == textSizePx && selectionSize != 0) Effects.FONTSIZE.clearFormattingInSelection(rtEditText);
			else rtManager.onEffectSelected(Effects.FONTSIZE, nextSize);
			updateActiveButtons();
		});
		binding.actionFontDecrease.setOnClickListener(v -> {
			List<Integer> fontSizes = Effects.FONTSIZE.valuesInSelection(rtEditText);
			int currSize = fontSizes.isEmpty() ? textSizePx : fontSizes.get(0);
			int nextSize = currSize > 20 ? currSize-1 : currSize;

			int selectionSize = rtEditText.getSelectionEnd() - rtEditText.getSelectionStart();
			if(nextSize == textSizePx && selectionSize != 0) Effects.FONTSIZE.clearFormattingInSelection(rtEditText);
			else rtManager.onEffectSelected(Effects.FONTSIZE, nextSize);
			updateActiveButtons();
		});
		binding.actionSuperscript.setOnClickListener(v -> {
			boolean isSuperscript = Effects.SUPERSCRIPT.existsInSelection(rtEditText);
			rtManager.onEffectSelected(Effects.SUPERSCRIPT, !isSuperscript);
			updateActiveButtons();
		});
		binding.actionSubscript.setOnClickListener(v -> {
			boolean isSubscript = Effects.SUBSCRIPT.existsInSelection(rtEditText);
			rtManager.onEffectSelected(Effects.SUBSCRIPT, !isSubscript);
			updateActiveButtons();
		});


		binding.toolbarSwap2.setOnClickListener(v -> toolbarBottom2.setVisibility(View.GONE));
	}



	private void updateActiveButtons() {
		binding.actionBold.setSelected(Effects.BOLD.existsInSelection(rtEditText));
		binding.actionItalic.setSelected(Effects.ITALIC.existsInSelection(rtEditText));
		binding.actionUnderline.setSelected(Effects.UNDERLINE.existsInSelection(rtEditText));
		binding.actionStrikethrough.setSelected(Effects.STRIKETHROUGH.existsInSelection(rtEditText));


		List<Layout.Alignment> alignments = Effects.ALIGNMENT.valuesInSelection(rtEditText);
		binding.actionAlignLeft.setSelected(alignments.isEmpty() || alignments.get(0) == Layout.Alignment.ALIGN_NORMAL);
		binding.actionAlignCenter.setSelected(!alignments.isEmpty() && alignments.get(0) == Layout.Alignment.ALIGN_CENTER);
		binding.actionAlignRight.setSelected(!alignments.isEmpty() && alignments.get(0) == Layout.Alignment.ALIGN_OPPOSITE);

		List<Integer> fontSizes = Effects.FONTSIZE.valuesInSelection(rtEditText);
		binding.actionHeading1.setSelected(!fontSizes.isEmpty() && fontSizes.get(0) == (textSizePx*2));
		binding.actionHeading2.setSelected(!fontSizes.isEmpty() && fontSizes.get(0) == (int)(textSizePx*1.5));

		binding.actionSuperscript.setSelected(Effects.SUPERSCRIPT.existsInSelection(rtEditText));
		binding.actionSubscript.setSelected(Effects.SUBSCRIPT.existsInSelection(rtEditText));
	}
}
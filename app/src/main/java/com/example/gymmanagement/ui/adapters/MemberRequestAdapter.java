package com.example.gymmanagement.ui.adapters;

import android.content.Context;
import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemMemberRequestBinding;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.UiUtil;

import java.util.function.Consumer;

/** Yêu cầu đăng ký của hội viên (pending / approved / rejected / cancelled). */
public class MemberRequestAdapter extends SimpleAdapter<MembershipRequest, ItemMemberRequestBinding> {
    public MemberRequestAdapter(Consumer<MembershipRequest> onCancel) {
        super(ItemMemberRequestBinding::inflate, (b, r, pos) -> {
            Context c = b.getRoot().getContext();
            b.rqGym.setText(r.gymName);
            b.rqPackage.setText(c.getString(R.string.ms_package_line, r.packageName, Fmt.money(r.packagePrice)));
            b.rqMeta.setText(c.getString(R.string.rq_sent_at, Fmt.dateTime(r.createdAt)));
            int type;
            switch (r.status) {
                case MembershipRequest.APPROVED:
                    type = UiUtil.BADGE_SUCCESS;
                    break;
                case MembershipRequest.PENDING:
                    type = UiUtil.BADGE_WARNING;
                    break;
                case MembershipRequest.REJECTED:
                    type = UiUtil.BADGE_PRIMARY;
                    break;
                default:
                    type = UiUtil.BADGE_NEUTRAL;
            }
            UiUtil.badge(b.rqStatus, r.statusLabel(), type);
            boolean showReason = MembershipRequest.REJECTED.equals(r.status) && !r.rejectReason.isEmpty();
            b.rqReason.setVisibility(showReason ? View.VISIBLE : View.GONE);
            b.rqReason.setText(c.getString(R.string.rq_reject_reason, r.rejectReason));
            b.rqCancel.setVisibility(r.isPending() ? View.VISIBLE : View.GONE);
            b.rqCancel.setOnClickListener(v -> onCancel.accept(r));
        });
    }
}

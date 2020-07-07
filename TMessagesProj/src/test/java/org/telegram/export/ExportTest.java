package org.telegram.export;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

class ExportHelper {
    public static void getCompleteChat(int userId, MessagesController controller){
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = controller.getInputPeer(userId);
//        TODO
        req.limit = 50;
        req.offset_id = 0;
        req.offset_date = 0;
        controller.getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response != null) {
                final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                Log.i(ExportHelper.class.getName(),res.toString());
//                removeDeletedMessagesFromArray(dialogId, res.messages);
//                int mid = max_id;
//                if (offset_date != 0 && !res.messages.isEmpty()) {
//                    mid = res.messages.get(res.messages.size() - 1).id;
//                    for (int a = res.messages.size() - 1; a >= 0; a--) {
//                        TLRPC.Message message = res.messages.get(a);
//                        if (message.date > offset_date) {
//                            mid = message.id;
//                            break;
//                        }
//                    }
//                }
//                processLoadedMessages(res, dialogId, mergeDialogId, count, mid, offset_date, false, classGuid, first_unread, last_message_id, unread_count, last_date, load_type, isChannel, false, false, loadIndex, queryFromServer, mentionsCount);
            } else {
                Log.e(ExportHelper.class.getName(),"response is null");
            }
        });

    }
}

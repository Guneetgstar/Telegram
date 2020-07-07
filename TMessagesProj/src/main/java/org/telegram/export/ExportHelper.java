package org.telegram.export;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.FileLoadOperation;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.UUID;

import static org.telegram.export.Database.getDatabase;

public class ExportHelper {
    public static void getCompleteChat(int userId, Context context, MessagesController controller){
        int pageSize=50;
        getDatabase(context).getMessageDao()
                .deleteAll();
//        getAppUserDir(context, ((long) userId)).delete();
        getAppCacheDir(context).delete();
        new Thread(()-> {
            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = controller.getInputPeer(userId);
            req.limit = pageSize;
            req.offset_id = 0;
            req.offset_date = 0;
            getPageChat(context, controller.getConnectionsManager(), req);
        }).start();
    }

    public static void getPageChat(Context context, ConnectionsManager connectionsManager, TLRPC.TL_messages_getHistory history) {
        connectionsManager.sendRequest(history, (response, error) -> {
            if (response != null) {
                final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                Log.i(ExportHelper.class.getName(), res.toString());
                if(res.messages.size()<=history.limit){
                    history.offset_id=res.messages.get(res.messages.size()-1).id;
                    for (TLRPC.Message message: res.messages) {
                        String filePath=null;
                        String actionName=null;
                        if(message.media!=null){
                            File localFile=null;
                            if(message.media.audio_unused !=null){
                                MessageObject object=new MessageObject(UserConfig.selectedAccount, message, false);
                                FileLoadOperation operation = new FileLoadOperation(object.getDocument(),object);
                                localFile=new File(getAppDir(context).getPath()+"/"+ asyncDownloadFile(operation,message.id,getAppDir(context),getAppCacheDir(context)));
                            }if(message.media.video_unused!=null){
                                MessageObject object=new MessageObject(UserConfig.selectedAccount, message, false);
                                FileLoadOperation operation = new FileLoadOperation(object.getDocument(),object);
                                localFile=new File(getAppDir(context).getPath()+"/"+ asyncDownloadFile(operation,message.id,getAppDir(context),getAppCacheDir(context)));
                            }if(message.media.photo!=null){
                                TLRPC.PhotoSize size=null;
                                for (TLRPC.PhotoSize tempSize:message.media.photo.sizes){
                                    if(size==null)
                                        size=tempSize;
                                    if(size.size<tempSize.size)
                                        size=tempSize;
                                }
                                ImageLocation location = ImageLocation.getForPhoto(size, message.media.photo);
                                if (location != null) {
                                    FileLoadOperation operation = new FileLoadOperation(location, new MessageObject(UserConfig.selectedAccount, message, false),null,location.getSize());
                                    localFile=new File(getAppDir(context).getPath()+"/"+ asyncDownloadFile(operation,message.id,getAppDir(context),getAppCacheDir(context)));
                                }
                            }if(message.media.document!=null){
                                TLRPC.TL_documentAttributeSticker attributeSticker=null;
                                for(TLRPC.DocumentAttribute attribute:message.media.document.attributes){
                                    if(attribute instanceof TLRPC.TL_documentAttributeSticker){
                                        attributeSticker=(TLRPC.TL_documentAttributeSticker) attribute;
                                        message.message += attributeSticker.alt;
                                    }
                                }
                                if(attributeSticker==null) {
                                    //Retain the name of the document and save the file
                                    message.message += FileLoader.getDocumentFileName(message.media.document);
                                    FileLoadOperation operation = new FileLoadOperation(message.media.document, new MessageObject(UserConfig.selectedAccount, message, false));
                                    localFile=new File(getAppDir(context).getPath()+"/"+ asyncDownloadFile(operation,message.id,getAppDir(context),getAppCacheDir(context)));
                                }
                            }if(message.media instanceof TLRPC.TL_messageMediaContact){
                                localFile=getAppFile(context,
                                        String.valueOf(message.id),
                                        "vcard");
                                try(FileWriter fileWriter=new FileWriter(localFile)){
                                    fileWriter.append(message.media.vcard);
                                }catch (Exception e){
                                    throw new RuntimeException(e.getMessage());
                                }
                            }
                            if(message.media.geo!=null){
                                message.message+="lat/long "+(message.media.geo.lat)+"/"+(message.media.geo._long);
                            }
                            if(localFile==null && (message.message==null||message.message.isEmpty())){
                                localFile=getAppFile(context,String.valueOf(message.id),"err");
                                try (FileWriter fileWriter=new FileWriter(localFile)){
                                    fileWriter.append("Cant read msg id "+message.id+" media_type "+message.media.getClass());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (localFile != null)
                                filePath = localFile.getPath();
                        }
                        if(message.action!=null){
                            actionName=message.action.getClass().getSimpleName();
                        }
                        getDatabase(context).getMessageDao()
                                .insert(new Message(message.id,
                                        message.from_id,
                                        message.to_id.user_id,
                                        message.reply_to_msg_id,
                                        message.date,
                                        getUserName(res.users,message.from_id),
                                        getUserName(res.users,message.to_id.user_id),
                                        message.message,
                                        actionName,
                                        filePath
                                        ));
                    }
                }if(res.messages.size()==history.limit){
                    getPageChat(context,connectionsManager,history);
                }
            } else {
                Log.e(ExportHelper.class.getName(), "response is null");
            }
        });
    }
    public static String getUserName(List<TLRPC.User> users,int userId){
        for(TLRPC.User user:users){
            if(user.id==userId){
                if(user.first_name!=null || user.last_name!=null) {
                    if(user.first_name!=null && user.last_name!=null)
                        return user.first_name+" "+user.last_name;
                    return (user.first_name!=null)?user.first_name:user.last_name;
                }
                if(user.phone!=null)
                    return user.phone;
                if(user.username!=null)
                    return user.username;
            }

        }
        return null;
    }
    public static String asyncDownloadFile(FileLoadOperation operation,int localId,File finalDir,File tempDir){
        String fileName=localId+operation.getFileName().substring(operation.getFileName().lastIndexOf("."));
        operation.setPaths(UserConfig.selectedAccount, finalDir, tempDir);
        operation.setDelegate(new FileLoadOperation.FileLoadOperationDelegate() {
            @Override
            public void didFinishLoadingFile(FileLoadOperation operation, File finalFile) {
                Log.i("File", "Loaded at " + finalFile.toString());
                while (!finalFile.renameTo(new File(finalDir,fileName))){
//                    continue;
                }
                tempDir.delete();
            }

            @Override
            public void didFailedLoadingFile(FileLoadOperation operation, int state) {
                Log.i("File", "Failed with state " + state);
                tempDir.delete();
            }

            @Override
            public void didChangedLoadProgress(FileLoadOperation operation, long uploadedSize, long totalSize) {
                Log.i("File", "Loaded " + uploadedSize + " of " + totalSize);
            }
        });
        operation.start();
        return fileName;
    }

    public static File getAppPrivateDir(@NonNull Context context) {
        return context.getFilesDir();
    }

    public static File getAppDir(Context application) {
        String state = Environment.getExternalStorageState();
        File file;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            file = new File(Environment.getExternalStorageDirectory() + "/Telegram Exports");
        } else {
            file = new File(application.getFilesDir() + "/Telegram Exports");
        }
        file.mkdirs();
        return file;
    }

    public static File getAppCacheDir(Context application){
        File file =new File(getAppDir(application), "cache/"+UUID.randomUUID().toString());
        file.deleteOnExit();
        file.mkdirs();
        return file;
    }

    public static File getAppFile(Context context, String fileName, String extension) {
        return new File(getAppDir(context), fileName + "." + extension);
    }

    private static File[] getAppDirs(Context application) {
        File[] files = new File[2];
        files[0] = new File(Environment.getExternalStorageDirectory() + "/Telegram Exports");
        files[1] = new File(application.getFilesDir() + "/Telegram Exports");
        return files;
    }

}

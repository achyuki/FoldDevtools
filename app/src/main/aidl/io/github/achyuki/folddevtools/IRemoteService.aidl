package io.github.achyuki.folddevtools;

import android.os.ParcelFileDescriptor;

interface IRemoteService {
    int getUid();
    List<String> getRemoteDevtoolsList();
    @nullable String getPackageNameByPid(int pid);
    void bindLocalSocketBridgeAsync(String socketName, in ParcelFileDescriptor bridgeSocket);
}

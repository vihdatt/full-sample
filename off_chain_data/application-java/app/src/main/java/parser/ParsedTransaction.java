/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.peer.TransactionPackage;

final class ParsedTransaction implements Transaction {
    private final ParsedPayload payload;
    private final AtomicReference<List<NamespaceReadWriteSet>> cachedNamespaceReadWriteSets = new AtomicReference<>();

    ParsedTransaction(final ParsedPayload payload) {
        this.payload = payload;
    }

    @Override
    public Common.ChannelHeader getChannelHeader() throws InvalidProtocolBufferException {
        return payload.getChannelHeader();
    }

    @Override
    public TransactionPackage.TxValidationCode getValidationCode() {
        return payload.getValidationCode();
    }

    @Override
    public boolean isValid() {
        return payload.isValid();
    }

    @Override
    public List<NamespaceReadWriteSet> getNamespaceReadWriteSets() throws InvalidProtocolBufferException {
        return Utils.getCachedProto(cachedNamespaceReadWriteSets, () -> new ArrayList<>(getReadWriteSets()));
    }

    @Override
    public Common.Payload toProto() {
        return payload.toProto();
    }

    private List<ParsedReadWriteSet> getReadWriteSets() throws InvalidProtocolBufferException {
        List<ParsedReadWriteSet> results = new ArrayList<>();
        for (ParsedTransactionAction action : getTransactionActions()) {
            results.addAll(action.getReadWriteSets());
        }

        return results;
    }

    private List<ParsedTransactionAction> getTransactionActions() throws InvalidProtocolBufferException {
        return getTransaction().getActionsList().stream()
                .map(ParsedTransactionAction::new)
                .collect(Collectors.toList());
    }

    private TransactionPackage.Transaction getTransaction() throws InvalidProtocolBufferException {
        return TransactionPackage.Transaction.parseFrom(payload.toProto().getData());
    }
}

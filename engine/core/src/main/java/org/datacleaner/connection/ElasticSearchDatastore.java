/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.connection;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.util.List;

import org.apache.metamodel.elasticsearch.ElasticSearchDataContext;
import org.apache.metamodel.util.SimpleTableDef;
import org.datacleaner.util.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;

/**
 * Datastore providing access to an ElasticSearch index.
 */
public class ElasticSearchDatastore extends UsageAwareDatastore<ElasticSearchDataContext> implements
        UpdateableDatastore {

    public enum ClientType {
        NODE, TRANSPORT
    }
    
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PORT = 9300;

    private final SimpleTableDef[] _tableDefs;
    private final String _indexName;
    private final String _hostname;
    private final int _port;
    private final String _clusterName;
    private final String _username;
    private final String _password;
    private final ClientType _clientType;

    public ElasticSearchDatastore(String name, String hostname, int port, String clusterName, String indexName,
            ClientType clientType) {
        this(name, hostname, port, clusterName, indexName, null, null, null, clientType);
    }

    public ElasticSearchDatastore(String name, String hostname, int port, String clusterName, String indexName,
            String username, String password, ClientType clientType) {
        this(name, hostname, port, clusterName, indexName, null, username, password, clientType);
    }

    public ElasticSearchDatastore(String name, String hostname, int port, String clusterName, String indexName,
            SimpleTableDef[] tableDefs, String username, String password, ClientType clientType) {
        super(name);
        _hostname = hostname;
        _port = port;
        _clusterName = clusterName;
        _indexName = indexName;
        _tableDefs = tableDefs;
        _username = username;
        _password = password;
        _clientType = clientType;
    }

    @Override
    public PerformanceCharacteristics getPerformanceCharacteristics() {
        return new PerformanceCharacteristicsImpl(true, false);
    }

    @Override
    protected UsageAwareDatastoreConnection<ElasticSearchDataContext> createDatastoreConnection() {

        Client client;
        if (ClientType.TRANSPORT.equals(_clientType)) {
            final Builder settingsBuilder = ImmutableSettings.builder();
            settingsBuilder.put("name", "AnalyzerBeans");
            settingsBuilder.put("cluster.name", _clusterName);
            if (!StringUtils.isNullOrEmpty(_username) && !StringUtils.isNullOrEmpty(_password)) {
                settingsBuilder.put("shield.user", _username + ":" + _password);
            }
            final Settings settings = settingsBuilder.build();

            client = new TransportClient(settings);
            ((TransportClient) client).addTransportAddress(new InetSocketTransportAddress(_hostname, _port));
        } else {
            // .client(true) means no shards are stored on this node
            final Node node = nodeBuilder().clusterName(_clusterName).client(true).node();
            client = node.client();
        }

        final ElasticSearchDataContext dataContext;
        if (_tableDefs == null || _tableDefs.length == 0) {
            dataContext = new ElasticSearchDataContext(client, _indexName);
        } else {
            dataContext = new ElasticSearchDataContext(client, _indexName, _tableDefs);
        }
        return new UpdateableDatastoreConnectionImpl<ElasticSearchDataContext>(dataContext, this);
    }

    @Override
    public UpdateableDatastoreConnection openConnection() {
        final DatastoreConnection connection = super.openConnection();
        return (UpdateableDatastoreConnection) connection;
    }

    public SimpleTableDef[] getTableDefs() {
        return _tableDefs;
    }

    public String getHostname() {
        return _hostname;
    }

    public int getPort() {
        return _port;
    }

    public String getClusterName() {
        return _clusterName;
    }

    public String getIndexName() {
        return _indexName;
    }

    public String getUsername() {
        return _username;
    }

    public String getPassword() {
        return _password;
    }

    @Override
    public String toString() {
        return "ElasticSearchDatastore[name=" + getName() + "]";
    }

    @Override
    protected void decorateIdentity(List<Object> identifiers) {
        super.decorateIdentity(identifiers);
        identifiers.add(_clusterName);
        identifiers.add(_hostname);
        identifiers.add(_indexName);
        identifiers.add(_tableDefs);
    }
}

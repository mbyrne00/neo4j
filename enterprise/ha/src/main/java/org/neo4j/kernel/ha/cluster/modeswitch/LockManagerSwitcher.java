/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.ha.cluster.modeswitch;

import org.neo4j.function.Factory;
import org.neo4j.kernel.AvailabilityGuard;
import org.neo4j.kernel.ha.DelegateInvocationHandler;
import org.neo4j.kernel.ha.com.RequestContextFactory;
import org.neo4j.kernel.ha.com.master.Master;
import org.neo4j.kernel.ha.lock.SlaveLockManager;
import org.neo4j.kernel.impl.locking.Locks;

public class LockManagerSwitcher extends AbstractComponentSwitcher<Locks>
{
    private final DelegateInvocationHandler<Master> master;
    private final RequestContextFactory requestContextFactory;
    private final AvailabilityGuard availabilityGuard;
    private final Factory<Locks> locksFactory;

    private volatile Locks currentLocks;

    public LockManagerSwitcher( DelegateInvocationHandler<Locks> delegate, DelegateInvocationHandler<Master> master,
            RequestContextFactory requestContextFactory, AvailabilityGuard availabilityGuard,
            Factory<Locks> locksFactory )
    {
        super( delegate );
        this.master = master;
        this.requestContextFactory = requestContextFactory;
        this.availabilityGuard = availabilityGuard;
        this.locksFactory = locksFactory;
    }

    @Override
    protected Locks getMasterImpl()
    {
        currentLocks = locksFactory.newInstance();
        return currentLocks;
    }

    @Override
    protected Locks getSlaveImpl()
    {
        currentLocks = new SlaveLockManager( locksFactory.newInstance(), requestContextFactory, master.cement(),
                availabilityGuard );
        return currentLocks;
    }

    @Override
    protected void shutdownCurrent()
    {
        super.shutdownCurrent();
        closeCurrentLocks();
    }

    private void closeCurrentLocks()
    {
        if ( currentLocks != null )
        {
            currentLocks.close();
            currentLocks = null;
        }
    }
}

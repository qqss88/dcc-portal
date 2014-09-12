ICGC DCC - Portal Openstack Deployemnet
===
### Deployment Dependency:
The templates and scripts found in here depend on the seqware-vagrant project from https://github.com/SeqWare/vagrant.
Please check out seqware-vagrant:

	git clone https://github.com/SeqWare/vagrant.git seqware-vagrant

copy the template directory found in here (src/main/seqware-vagrant/templates) to the seqware-vagrant/templates.

### ICGC DCC Portal for development 

This will spin up a standard, 2 node SeqWare cluster (using Oozie-Hadoop), will
setup elasticsearch, will download a dump of the (small) elasticsearch DCC
index, load the dump into elasticsearch, and launch the DCC Portal web app on
port 5381.

Keep in mind you should edit the json below before you launch to make sure your
floating IP addresses and other settings are correct.  Also, the specific index
dump file and DCC Portal jar file are hard coded in the provision scripts
referenced inside the JSON so you will want to change these if there's an
update.  Also, take a look at templates/DCC/settings.yml which has the index
name embedded and will need to change if the index is updated.

    # use this template, customize it
    cp templates/sample_configs/vagrant_cluster_launch.dcc_small_portal.cluster.json.template vagrant_cluster_launch.json
    # launch, use the correct command line args for you
    perl vagrant_cluster_launch.pl --use-openstack

Once this finishes launching you can browse the DCC Portal at http://\<master_node_IP\>:5381/.
